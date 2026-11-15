"""ONNX model loading, preprocessing and interpretation of the class probabilities."""
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageOps

Image.MAX_IMAGE_PIXELS = 40_000_000  # reject decompression bombs early


def softmax(logits: np.ndarray) -> np.ndarray:
    z = logits - logits.max(axis=-1, keepdims=True)
    e = np.exp(z)
    return e / e.sum(axis=-1, keepdims=True)


@dataclass
class Interpretation:
    plant_index: int
    plant_confidence: float
    recognized: bool
    condition_status: str  # assessed | uncertain | not_supported | unrecognized
    condition_index: int | None
    condition_confidence: float | None
    top_classes: list[tuple[int, float]]
    top_plants: list[tuple[int, float]]


class LabelSpace:
    """Class/plant metadata from labels.json, plus the rules that turn probabilities into an answer."""

    def __init__(self, meta: dict):
        labels = sorted(meta["labels"], key=lambda item: item["index"])
        plants = sorted(meta["plants"], key=lambda item: item["index"])
        if [item["index"] for item in labels] != list(range(len(labels))):
            raise ValueError("labels.json: label indices must be 0..N-1")
        self.meta = meta
        self.labels = labels
        self.plants = plants
        self.class_ids = [item["id"] for item in labels]
        self.plant_ids = [item["id"] for item in plants]
        self.class_plant = np.array([item["plant_index"] for item in labels], dtype=np.int64)
        self.plant_onehot = np.eye(len(plants), dtype=np.float32)[self.class_plant]  # (classes, plants)
        self.condition_supported = [bool(item["condition_supported"]) for item in plants]

    @property
    def num_classes(self) -> int:
        return len(self.labels)

    def interpret(self, probs: np.ndarray, plant_min: float, condition_min: float, top_k: int = 3) -> Interpretation:
        """Plant = sum of its class probabilities. The condition is reported only when the plant is
        recognized and the dataset covers both healthy and diseased leaves of that plant."""
        probs = np.asarray(probs, dtype=np.float64)
        plant_probs = probs @ self.plant_onehot
        plant_idx = int(plant_probs.argmax())
        plant_conf = float(plant_probs[plant_idx])
        own = np.flatnonzero(self.class_plant == plant_idx)
        best = int(own[probs[own].argmax()])
        best_conf = float(probs[best])

        recognized = plant_conf >= plant_min
        if not recognized:
            status, cond_idx, cond_conf = "unrecognized", None, None
        elif not self.condition_supported[plant_idx]:
            status, cond_idx, cond_conf = "not_supported", None, None
        else:
            status = "assessed" if best_conf >= condition_min else "uncertain"
            cond_idx, cond_conf = best, best_conf

        top_classes = [(int(i), float(probs[i])) for i in np.argsort(-probs)[:top_k]]
        top_plants = [(int(i), float(plant_probs[i])) for i in np.argsort(-plant_probs)[:top_k]]
        return Interpretation(plant_idx, plant_conf, recognized, status, cond_idx, cond_conf, top_classes, top_plants)


class Preprocessor:
    """Same steps as the training notebook's eval transform:
    Resize(shorter side) -> CenterCrop -> /255 -> normalize, RGB, NCHW float32."""

    def __init__(self, spec: dict):
        self.resize = int(spec["resize_shorter_side"])
        self.crop = int(spec["center_crop"])
        self.mean = np.array(spec["mean"], dtype=np.float32).reshape(3, 1, 1)
        self.std = np.array(spec["std"], dtype=np.float32).reshape(3, 1, 1)

    def resize_shorter(self, img: Image.Image) -> Image.Image:
        w, h = img.size
        if w <= h:
            new_w, new_h = self.resize, int(self.resize * h / w)
        else:
            new_w, new_h = int(self.resize * w / h), self.resize
        return img.resize((new_w, new_h), Image.BILINEAR)

    def center_crop(self, img: Image.Image) -> Image.Image:
        w, h = img.size
        left = int(round((w - self.crop) / 2.0))
        top = int(round((h - self.crop) / 2.0))
        return img.crop((left, top, left + self.crop, top + self.crop))

    def __call__(self, img: Image.Image) -> np.ndarray:
        img = ImageOps.exif_transpose(img).convert("RGB")
        img = self.center_crop(self.resize_shorter(img))
        x = np.asarray(img, dtype=np.float32).transpose(2, 0, 1) / 255.0
        x = (x - self.mean) / self.std
        return x[None].astype(np.float32)


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


class PlantClassifier:
    def __init__(self, model_path: Path, labels_path: Path, threads: int = 0):
        import onnxruntime as ort

        meta = json.loads(Path(labels_path).read_text())
        expected_sha = meta.get("model_sha256")
        actual_sha = sha256_of(Path(model_path))
        if expected_sha and expected_sha != actual_sha:
            raise RuntimeError(f"Model checksum mismatch: labels.json expects {expected_sha[:12]}, "
                               f"file is {actual_sha[:12]}. Model and labels.json come from different runs.")
        self.labels = LabelSpace(meta)
        self.preprocess = Preprocessor(meta["input"])
        self.version = actual_sha[:12]
        self.architecture = meta.get("architecture", "unknown")

        options = ort.SessionOptions()
        if threads > 0:
            options.intra_op_num_threads = threads
        self.session = ort.InferenceSession(str(model_path), options, providers=["CPUExecutionProvider"])
        self.input_name = self.session.get_inputs()[0].name
        out_dim = self.session.get_outputs()[0].shape[-1]
        if isinstance(out_dim, int) and out_dim != self.labels.num_classes:
            raise RuntimeError(f"Model outputs {out_dim} classes but labels.json has {self.labels.num_classes}")

    def predict_probs(self, img: Image.Image) -> np.ndarray:
        logits = self.session.run(None, {self.input_name: self.preprocess(img)})[0][0]
        return softmax(logits.astype(np.float64))
