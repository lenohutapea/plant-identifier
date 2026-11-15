# Plant Identifier API

FastAPI + ONNX Runtime service that classifies a leaf photo into crop + leaf condition
and returns Indonesian care information.

| Method | Path | Description |
|---|---|---|
| POST | `/predict` | Multipart field `image`. Returns plant, condition, top-3 classes, top-3 plants |
| GET | `/plants` | All supported plants and their conditions |
| GET | `/plants/{plant_id}` | Care information for one plant (Indonesian) |
| GET | `/healthz` | Liveness |
| GET | `/readyz` | Readiness (503 until the model is loaded) |
| GET | `/metrics` | Prometheus metrics |

Interactive docs: `/docs`.

## How a prediction is interpreted

- **Plant** = sum of the probabilities of that plant's classes. If it is below
  `PLANT_MIN_CONFIDENCE`, the response has `recognized: false` and `condition_status: "unrecognized"`.
- **Condition** is only reported for plants whose training data has both healthy and diseased leaves
  (`condition_supported` in `labels.json`). For the others the status is `"not_supported"`.
- If the best condition is below `CONDITION_MIN_CONFIDENCE`, the status is `"uncertain"`.

## Run locally

```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt
pytest
python -m tests.dummy_model models/      # stand-in model until the trained one is available
uvicorn app.main:app --reload
```

## Configuration

| Variable | Default |
|---|---|
| `MODEL_DIR` | `./models` (expects `plant_classifier.onnx` and `labels.json`) |
| `MODEL_PATH`, `LABELS_PATH`, `PLANTS_PATH` | derived from `MODEL_DIR` / `app/data/plants.json` |
| `MAX_UPLOAD_MB` | `8` |
| `PLANT_MIN_CONFIDENCE` | `0.5` |
| `CONDITION_MIN_CONFIDENCE` | `0.5` |
| `ORT_THREADS` | `0` (onnxruntime default) |

`labels.json` stores the SHA-256 of the model; the service refuses to start if they do not match.
