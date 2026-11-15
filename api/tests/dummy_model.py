"""Tiny stand-in for the real classifier, with the same input/output contract.

logits = mean_over_pixels(input) @ W + bias, so the bias decides the prediction.
Used by the tests, and for trying the API locally before the trained model exists:

    python -m tests.dummy_model models/
"""
from __future__ import annotations

import hashlib
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import numpy as np

PLANTS_JSON = Path(__file__).resolve().parents[1] / "app" / "data" / "plants.json"
MEAN = [0.485, 0.456, 0.406]
STD = [0.229, 0.224, 0.225]


def class_list(plants_json: Path = PLANTS_JSON) -> list[str]:
    plants = json.loads(plants_json.read_text(encoding="utf-8"))["plants"]
    return sorted(f"{p}___{c}" for p, info in plants.items() for c in info["conditions"])


def build_labels(classes: list[str], model_sha256: str) -> dict:
    """Same structure as labels.json written by the training notebook."""
    plants = sorted({c.split("___")[0] for c in classes})
    conditions = {p: sorted(c.split("___")[1] for c in classes if c.startswith(p + "___")) for p in plants}
    return {
        "model_file": "plant_classifier.onnx",
        "model_sha256": model_sha256,
        "architecture": "dummy",
        "created_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "num_classes": len(classes),
        "num_plants": len(plants),
        "labels": [{"index": i, "id": c, "plant": c.split("___")[0], "plant_index": plants.index(c.split("___")[0]),
                    "condition": c.split("___")[1]} for i, c in enumerate(classes)],
        "plants": [{"index": i, "id": p, "conditions": conditions[p],
                    "condition_supported": "Healthy" in conditions[p] and len(conditions[p]) >= 2}
                   for i, p in enumerate(plants)],
        "input": {"name": "input", "layout": "NCHW", "dtype": "float32", "color": "RGB",
                  "resize_shorter_side": 256, "center_crop": 224, "scale": "pixel / 255", "mean": MEAN, "std": STD},
        "output": {"name": "logits", "note": "apply softmax to get probabilities"},
    }


def build_onnx(path: Path, bias: np.ndarray) -> None:
    import onnx
    from onnx import TensorProto, helper, numpy_helper

    num_classes = len(bias)
    weights = np.zeros((3, num_classes), dtype=np.float32)
    nodes = [
        helper.make_node("ReduceMean", ["input"], ["pooled"], axes=[2, 3], keepdims=0),
        helper.make_node("MatMul", ["pooled", "W"], ["scores"]),
        helper.make_node("Add", ["scores", "b"], ["logits"]),
    ]
    graph = helper.make_graph(
        nodes, "dummy_plant_classifier",
        [helper.make_tensor_value_info("input", TensorProto.FLOAT, ["batch", 3, 224, 224])],
        [helper.make_tensor_value_info("logits", TensorProto.FLOAT, ["batch", num_classes])],
        initializer=[numpy_helper.from_array(weights, "W"),
                     numpy_helper.from_array(bias.astype(np.float32), "b")],
    )
    model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 13)], producer_name="dummy")
    model.ir_version = 8
    onnx.checker.check_model(model)
    onnx.save(model, str(path))


def write_model(out_dir: Path, bias_by_label: dict[str, float] | None = None, default_bias: float = 0.0,
                classes: list[str] | None = None, sha_override: str | None = None) -> tuple[Path, Path]:
    """Write plant_classifier.onnx + labels.json into out_dir. Returns (model_path, labels_path)."""
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    classes = classes or class_list()
    bias = np.full(len(classes), default_bias, dtype=np.float32)
    for label, value in (bias_by_label or {}).items():
        bias[classes.index(label)] = value
    model_path = out_dir / "plant_classifier.onnx"
    build_onnx(model_path, bias)
    sha = sha_override or hashlib.sha256(model_path.read_bytes()).hexdigest()
    labels_path = out_dir / "labels.json"
    labels_path.write_text(json.dumps(build_labels(classes, sha), indent=2))
    return model_path, labels_path


if __name__ == "__main__":
    target = Path(sys.argv[1] if len(sys.argv) > 1 else "models")
    model, labels = write_model(target, {"Tomato___Early_blight": 6.0})
    print(f"Dummy model written: {model} and {labels} (always predicts Tomato___Early_blight)")
