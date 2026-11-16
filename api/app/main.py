"""Plant Identifier API: crop + leaf condition classification with Indonesian care information."""
from __future__ import annotations

import io
import logging
import time
import warnings
from contextlib import asynccontextmanager

from fastapi import FastAPI, File, HTTPException, Request, UploadFile
from fastapi.responses import JSONResponse, Response
from PIL import Image, UnidentifiedImageError
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest
from starlette.routing import Match

from . import metrics
from .config import Settings
from .model import PlantClassifier
from .plants import PlantStore
from .schemas import HealthResponse, PredictResponse, ReadyResponse

logger = logging.getLogger("plant_identifier")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

API_VERSION = "0.1.0"


def route_template(request: Request) -> str:
    """Route path template (e.g. /plants/{plant_id}) to keep metric label cardinality low."""
    route = request.scope.get("route")
    if route is not None:
        return route.path
    for candidate in request.app.routes:
        match, _ = candidate.matches(request.scope)
        if match == Match.FULL:
            return candidate.path
    return "unmatched"


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or Settings.from_env()

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        app.state.settings = settings
        app.state.plants = PlantStore(settings.plants_path)
        app.state.classifier = None
        metrics.MODEL_READY.set(0)
        if settings.model_path.exists() and settings.labels_path.exists():
            classifier = PlantClassifier(settings.model_path, settings.labels_path, settings.ort_threads)
            missing = app.state.plants.missing_for(classifier.labels.class_ids)
            if missing:
                raise RuntimeError(f"plants.json has no entry for model classes: {missing}")
            app.state.classifier = classifier
            metrics.MODEL_INFO.labels(classifier.version, classifier.architecture,
                                      str(classifier.labels.num_classes)).set(1)
            metrics.MODEL_READY.set(1)
            logger.info("Model %s loaded (%d classes)", classifier.version, classifier.labels.num_classes)
        else:
            logger.error("Model files not found (%s, %s); /readyz will report not ready",
                         settings.model_path, settings.labels_path)
        yield

    app = FastAPI(
        title="Plant Identifier API",
        version=API_VERSION,
        description="Identifies the crop and leaf condition from a photo of a leaf and returns care "
                    "information in Indonesian.",
        lifespan=lifespan,
    )

    @app.middleware("http")
    async def record_metrics(request: Request, call_next):
        start = time.perf_counter()
        status = 500
        try:
            response = await call_next(request)
            status = response.status_code
            return response
        finally:
            path = route_template(request)
            if path != "/metrics":
                metrics.HTTP_REQUESTS.labels(request.method, path, str(status)).inc()
                metrics.HTTP_LATENCY.labels(request.method, path).observe(time.perf_counter() - start)

    @app.get("/healthz", response_model=HealthResponse, tags=["ops"])
    def healthz():
        """Liveness: the process is up."""
        return {"status": "ok"}

    @app.get("/readyz", response_model=ReadyResponse, tags=["ops"],
             responses={503: {"model": ReadyResponse}})
    def readyz(request: Request):
        """Readiness: the model is loaded and can serve predictions."""
        classifier = request.app.state.classifier
        if classifier is None:
            return JSONResponse(status_code=503, content={"status": "model not loaded"})
        return {"status": "ready", "model_version": classifier.version}

    @app.get("/metrics", include_in_schema=False)
    def prometheus_metrics():
        return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)

    @app.get("/plants", tags=["plants"])
    def list_plants(request: Request):
        """Plants the loaded model can recognize, with their conditions."""
        store: PlantStore = request.app.state.plants
        summaries = store.summaries()
        classifier = request.app.state.classifier
        if classifier is not None:
            # Only advertise plants the loaded model can actually recognize.
            known = set(classifier.labels.plant_ids)
            summaries = [p for p in summaries if p["id"] in known]
        return {"disclaimer": store.disclaimer, "plants": summaries}

    @app.get("/plants/{plant_id}", tags=["plants"])
    def get_plant(plant_id: str, request: Request):
        """Full care information for one plant (Indonesian), including all its conditions."""
        store: PlantStore = request.app.state.plants
        plant = store.get(plant_id)
        if plant is None:
            raise HTTPException(status_code=404, detail=f"Unknown plant id: {plant_id}")
        return {"disclaimer": store.disclaimer, **plant}

    @app.post("/predict", response_model=PredictResponse, tags=["predict"],
              responses={400: {"description": "Not a valid image"}, 413: {"description": "Image too large"},
                         503: {"description": "Model not loaded"}})
    def predict(request: Request, image: UploadFile = File(..., description="Leaf photo (JPEG/PNG/WebP)")):
        """Classify a leaf photo. Returns the plant, the leaf condition (when it can be assessed),
        the top-3 classes and the top-3 plants."""
        classifier: PlantClassifier | None = request.app.state.classifier
        store: PlantStore = request.app.state.plants
        if classifier is None:
            raise HTTPException(status_code=503, detail="Model not loaded")

        data = image.file.read(settings.max_upload_bytes + 1)
        if len(data) > settings.max_upload_bytes:
            raise HTTPException(status_code=413,
                                detail=f"Image larger than {settings.max_upload_bytes // (1024 * 1024)} MB")
        try:
            with warnings.catch_warnings():
                warnings.simplefilter("error", Image.DecompressionBombWarning)
                img = Image.open(io.BytesIO(data))
                img.load()
        except (UnidentifiedImageError, OSError, Image.DecompressionBombError, Image.DecompressionBombWarning):
            raise HTTPException(status_code=400, detail="File is not a supported or valid image")

        start = time.perf_counter()
        probs = classifier.predict_probs(img)
        elapsed = time.perf_counter() - start
        metrics.INFERENCE_LATENCY.observe(elapsed)

        labels = classifier.labels
        result = labels.interpret(probs, settings.plant_min_confidence, settings.condition_min_confidence)
        plant_id = labels.plant_ids[result.plant_index]
        plant_info = store.get(plant_id)

        condition = None
        if result.condition_index is not None:
            label = labels.class_ids[result.condition_index]
            condition_id = label.split("___", 1)[1]
            info = plant_info["conditions"][condition_id]
            condition = {"id": condition_id, "label": label, "name": info["name"], "type": info["type"],
                         "confidence": result.condition_confidence}

        def class_entry(idx: int, conf: float) -> dict:
            label = labels.class_ids[idx]
            pid, cid = label.split("___", 1)
            p = store.get(pid)
            return {"label": label, "plant_id": pid, "plant_name": p["name"], "condition_id": cid,
                    "condition_name": p["conditions"][cid]["name"], "confidence": conf}

        metrics.PREDICTIONS.labels(plant_id if result.recognized else "unrecognized", result.condition_status).inc()
        return {
            "recognized": result.recognized,
            "plant": {"id": plant_id, "name": plant_info["name"], "confidence": result.plant_confidence},
            "condition_status": result.condition_status,
            "condition": condition,
            "top_predictions": [class_entry(i, c) for i, c in result.top_classes],
            "top_plants": [{"id": labels.plant_ids[i], "name": store.get(labels.plant_ids[i])["name"],
                            "confidence": c} for i, c in result.top_plants],
            "model_version": classifier.version,
            "inference_ms": round(elapsed * 1000, 2),
            "disclaimer": store.disclaimer,
        }

    return app


app = create_app()
