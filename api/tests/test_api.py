from __future__ import annotations

import pytest

from tests.conftest import jpeg_bytes


def post_image(client, data: bytes, filename="leaf.jpg", content_type="image/jpeg"):
    return client.post("/predict", files={"image": (filename, data, content_type)})


# ---------- ops ----------

def test_healthz(client):
    r = client.get("/healthz")
    assert r.status_code == 200
    assert r.json() == {"status": "ok"}


def test_readyz_when_model_loaded(client):
    r = client.get("/readyz")
    assert r.status_code == 200
    assert r.json()["status"] == "ready"
    assert len(r.json()["model_version"]) == 12


def test_not_ready_without_model(make_client, leaf_jpeg):
    client = make_client(write=False)
    assert client.get("/healthz").status_code == 200
    assert client.get("/readyz").status_code == 503
    assert post_image(client, leaf_jpeg).status_code == 503


def test_startup_fails_on_checksum_mismatch(make_client):
    with pytest.raises(RuntimeError, match="checksum mismatch"):
        make_client({"Tomato___Healthy": 5.0}, sha_override="0" * 64)


def metric_samples(client) -> list:
    """All samples from /metrics, parsed (independent of label order in the text format)."""
    from prometheus_client.parser import text_string_to_metric_families
    text = client.get("/metrics").text
    return [s for family in text_string_to_metric_families(text) for s in family.samples]


def has_sample(samples, name: str, **labels) -> bool:
    return any(s.name == name and all(s.labels.get(k) == v for k, v in labels.items()) for s in samples)


def test_metrics_after_prediction(client, leaf_jpeg):
    assert post_image(client, leaf_jpeg).status_code == 200
    samples = metric_samples(client)
    assert has_sample(samples, "http_requests_total", method="POST", route="/predict", status="200")
    assert has_sample(samples, "predictions_total", plant="Tomato", condition_status="assessed")
    assert has_sample(samples, "model_inference_duration_seconds_bucket")
    assert any(s.name == "model_ready" and s.value == 1.0 for s in samples)


def test_metrics_use_route_template(client):
    client.get("/plants/Tomato")
    client.get("/plants/Corn")
    samples = metric_samples(client)
    assert has_sample(samples, "http_requests_total", route="/plants/{plant_id}")
    assert not has_sample(samples, "http_requests_total", route="/plants/Tomato")


# ---------- /predict ----------

def test_predict_confident_diagnosis(client, leaf_jpeg):
    r = post_image(client, leaf_jpeg)
    assert r.status_code == 200
    body = r.json()
    assert body["recognized"] is True
    assert body["plant"]["id"] == "Tomato" and body["plant"]["name"] == "Tomat"
    assert body["plant"]["confidence"] > 0.99
    assert body["condition_status"] == "assessed"
    assert body["condition"]["id"] == "Early_blight"
    assert body["condition"]["label"] == "Tomato___Early_blight"
    assert body["condition"]["name"] == "Bercak kering"
    assert len(body["top_predictions"]) == 3
    assert body["top_predictions"][0]["label"] == "Tomato___Early_blight"
    assert len(body["top_plants"]) == 3
    assert body["disclaimer"]
    assert body["inference_ms"] >= 0


def test_predict_plant_without_condition_support(make_client, leaf_jpeg):
    client = make_client({"Orange___Citrus_greening": 10.0})
    body = post_image(client, leaf_jpeg).json()
    assert body["recognized"] is True
    assert body["plant"]["id"] == "Orange"
    assert body["condition_status"] == "not_supported"
    assert body["condition"] is None


def test_predict_uncertain_condition(make_client, leaf_jpeg):
    # all 10 tomato classes equally likely: plant is clear, disease is not
    tomato = {f"Tomato___{c}": 3.0 for c in ["Bacterial_spot", "Early_blight", "Late_blight", "Leaf_mold",
                                             "Septoria_leaf_spot", "Spider_mites", "Target_spot",
                                             "Yellow_leaf_curl_virus", "Mosaic_virus", "Healthy"]}
    client = make_client(tomato)
    body = post_image(client, leaf_jpeg).json()
    assert body["recognized"] is True
    assert body["plant"]["id"] == "Tomato"
    assert body["plant"]["confidence"] > 0.8
    assert body["condition_status"] == "uncertain"
    assert body["condition"]["confidence"] < 0.5


def test_predict_unrecognized(make_client, leaf_jpeg):
    client = make_client()  # all logits equal -> no plant reaches 50%
    body = post_image(client, leaf_jpeg).json()
    assert body["recognized"] is False
    assert body["condition_status"] == "unrecognized"
    assert body["condition"] is None


def test_predict_accepts_png_and_portrait(client):
    import io
    from PIL import Image
    buf = io.BytesIO()
    Image.new("RGB", (300, 900), (90, 160, 70)).save(buf, "PNG")
    assert post_image(client, buf.getvalue(), "leaf.png", "image/png").status_code == 200


def test_predict_rejects_non_image(client):
    r = post_image(client, b"this is not an image", "notes.txt", "text/plain")
    assert r.status_code == 400


def test_predict_rejects_large_upload(make_client):
    client = make_client({"Tomato___Healthy": 5.0}, max_upload_bytes=10_000)
    r = post_image(client, jpeg_bytes(size=(1200, 900)))
    assert r.status_code == 413


def test_predict_requires_image_field(client):
    assert client.post("/predict").status_code == 422


# ---------- /plants ----------

def test_list_plants(client):
    body = client.get("/plants").json()
    ids = [p["id"] for p in body["plants"]]
    assert len(ids) == 15
    assert "Rice" in ids and "Tomato" in ids
    unsupported = sorted(p["id"] for p in body["plants"] if not p["condition_supported"])
    assert unsupported == ["Blueberry", "Orange", "Raspberry", "Rice", "Soybean", "Squash"]


def test_get_plant(client):
    r = client.get("/plants/Tomato")
    assert r.status_code == 200
    body = r.json()
    assert body["name"] == "Tomat"
    assert len(body["conditions"]) == 10
    assert body["schedule"][0]["hst"] < 0  # seeding before transplanting
    assert body["disclaimer"]


def test_get_unknown_plant(client):
    assert client.get("/plants/Durian").status_code == 404
