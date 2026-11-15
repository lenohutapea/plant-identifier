"""Unit tests for preprocessing and the probability -> answer rules (no HTTP)."""
from __future__ import annotations

import numpy as np
import pytest
from PIL import Image

from app.config import APP_DIR
from app.model import LabelSpace, PlantClassifier, Preprocessor, softmax
from app.plants import PlantStore
from tests.dummy_model import build_labels, class_list, write_model

CLASSES = class_list()
SPEC = build_labels(CLASSES, "")["input"]


@pytest.fixture(scope="module")
def space():
    return LabelSpace(build_labels(CLASSES, ""))


def probs_for(weights: dict[str, float]) -> np.ndarray:
    p = np.full(len(CLASSES), 1e-6)
    for label, value in weights.items():
        p[CLASSES.index(label)] = value
    return p / p.sum()


def test_plants_json_covers_every_class():
    assert PlantStore(APP_DIR / "data" / "plants.json").missing_for(CLASSES) == []
    assert len(CLASSES) == 41


def test_softmax_is_stable():
    p = softmax(np.array([1000.0, 1000.0, -1000.0]))
    assert p == pytest.approx([0.5, 0.5, 0.0])


def test_plant_probability_is_sum_of_its_classes(space):
    # no single class is above 35%, but Tomato as a plant is 70%
    probs = probs_for({"Tomato___Early_blight": 0.32, "Tomato___Late_blight": 0.23, "Tomato___Healthy": 0.15,
                       "Potato___Early_blight": 0.30})
    result = space.interpret(probs, plant_min=0.5, condition_min=0.5)
    assert space.plant_ids[result.plant_index] == "Tomato"
    assert result.plant_confidence == pytest.approx(0.70, abs=1e-3)
    assert result.condition_status == "uncertain"
    assert CLASSES[result.condition_index] == "Tomato___Early_blight"
    assert CLASSES[result.top_classes[0][0]] == "Tomato___Early_blight"


@pytest.mark.parametrize("label,status", [
    ("Tomato___Late_blight", "assessed"),
    ("Rice___Blast", "not_supported"),
    ("Soybean___Healthy", "not_supported"),
    ("Squash___Powdery_mildew", "not_supported"),
])
def test_condition_status(space, label, status):
    result = space.interpret(probs_for({label: 0.95}), plant_min=0.5, condition_min=0.5)
    assert space.plant_ids[result.plant_index] == label.split("___")[0]
    assert result.condition_status == status
    assert (result.condition_index is None) == (status == "not_supported")


def test_unrecognized_below_plant_threshold(space):
    result = space.interpret(np.full(len(CLASSES), 1 / len(CLASSES)), plant_min=0.5, condition_min=0.5)
    assert not result.recognized
    assert result.condition_status == "unrecognized"


def test_preprocess_shape_and_normalization():
    x = Preprocessor(SPEC)(Image.new("RGB", (400, 300), (128, 128, 128)))
    assert x.shape == (1, 3, 224, 224) and x.dtype == np.float32
    expected = (128 / 255 - np.array(SPEC["mean"])) / np.array(SPEC["std"])
    assert x[0, :, 100, 100] == pytest.approx(expected, abs=1e-5)


def test_preprocess_resize_and_center_crop_geometry():
    pre = Preprocessor(SPEC)
    assert pre.resize_shorter(Image.new("RGB", (400, 300))).size == (341, 256)   # landscape
    assert pre.resize_shorter(Image.new("RGB", (300, 900))).size == (256, 768)   # portrait
    # a marker in the image centre must stay in the crop centre
    img = Image.new("RGB", (640, 480), (0, 0, 0))
    img.paste((255, 255, 255), (310, 230, 330, 250))
    x = pre(img)
    assert x[0, 0, 112, 112] > 1.5 and x[0, 0, 5, 5] < -1.5


def test_preprocess_applies_exif_rotation():
    img = Image.new("RGB", (400, 200), (10, 200, 10))
    exif = img.getexif()
    exif[0x0112] = 6  # rotate 90 degrees when displayed
    import io
    buf = io.BytesIO()
    img.save(buf, "JPEG", exif=exif.tobytes())
    rotated = Image.open(io.BytesIO(buf.getvalue()))
    from PIL import ImageOps
    assert ImageOps.exif_transpose(rotated).size == (200, 400)
    assert Preprocessor(SPEC)(rotated).shape == (1, 3, 224, 224)


def test_classifier_with_dummy_onnx(tmp_path):
    model_path, labels_path = write_model(tmp_path, {"Corn___Common_rust": 8.0})
    clf = PlantClassifier(model_path, labels_path)
    probs = clf.predict_probs(Image.new("RGB", (500, 400), (40, 120, 40)))
    assert probs.shape == (41,)
    assert probs.sum() == pytest.approx(1.0)
    assert clf.labels.class_ids[int(probs.argmax())] == "Corn___Common_rust"
