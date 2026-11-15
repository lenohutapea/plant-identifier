"""Runtime settings, read from environment variables."""
from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

APP_DIR = Path(__file__).resolve().parent
DEFAULT_MODEL_DIR = APP_DIR.parent / "models"


@dataclass(frozen=True)
class Settings:
    model_path: Path
    labels_path: Path
    plants_path: Path
    max_upload_bytes: int = 8 * 1024 * 1024
    plant_min_confidence: float = 0.5
    condition_min_confidence: float = 0.5
    ort_threads: int = 0  # 0 = let onnxruntime decide

    @classmethod
    def from_env(cls) -> "Settings":
        model_dir = Path(os.getenv("MODEL_DIR", str(DEFAULT_MODEL_DIR)))
        return cls(
            model_path=Path(os.getenv("MODEL_PATH", str(model_dir / "plant_classifier.onnx"))),
            labels_path=Path(os.getenv("LABELS_PATH", str(model_dir / "labels.json"))),
            plants_path=Path(os.getenv("PLANTS_PATH", str(APP_DIR / "data" / "plants.json"))),
            max_upload_bytes=int(float(os.getenv("MAX_UPLOAD_MB", "8")) * 1024 * 1024),
            plant_min_confidence=float(os.getenv("PLANT_MIN_CONFIDENCE", "0.5")),
            condition_min_confidence=float(os.getenv("CONDITION_MIN_CONFIDENCE", "0.5")),
            ort_threads=int(os.getenv("ORT_THREADS", "0")),
        )
