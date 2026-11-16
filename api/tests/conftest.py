from __future__ import annotations

import io
from dataclasses import replace
from pathlib import Path

import numpy as np
import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app.config import APP_DIR, Settings
from app.main import create_app
from tests.dummy_model import write_model


def jpeg_bytes(size=(640, 480), color=(60, 140, 60)) -> bytes:
    rng = np.random.default_rng(0)
    arr = np.clip(np.array(color) + rng.integers(-20, 20, (size[1], size[0], 3)), 0, 255).astype(np.uint8)
    buf = io.BytesIO()
    Image.fromarray(arr).save(buf, "JPEG", quality=90)
    return buf.getvalue()


@pytest.fixture
def make_client(tmp_path: Path):
    """make_client(bias_by_label, default_bias=0.0, **settings_overrides) -> TestClient (lifespan started)."""
    clients = []

    def factory(bias_by_label=None, default_bias=0.0, write=True, sha_override=None, classes=None, **overrides):
        model_dir = tmp_path / f"models_{len(clients)}"
        if write:
            write_model(model_dir, bias_by_label, default_bias, classes=classes, sha_override=sha_override)
        settings = Settings(model_path=model_dir / "plant_classifier.onnx",
                            labels_path=model_dir / "labels.json",
                            plants_path=APP_DIR / "data" / "plants.json")
        settings = replace(settings, **overrides)
        client = TestClient(create_app(settings))
        client.__enter__()
        clients.append(client)
        return client

    yield factory
    for client in clients:
        client.__exit__(None, None, None)


@pytest.fixture
def client(make_client):
    """Model that confidently predicts Tomato___Early_blight."""
    return make_client({"Tomato___Early_blight": 10.0})


@pytest.fixture
def leaf_jpeg() -> bytes:
    return jpeg_bytes()
