"""Prometheus metrics exposed on /metrics."""
from prometheus_client import Counter, Gauge, Histogram

HTTP_REQUESTS = Counter(
    "http_requests_total", "HTTP requests by method, route template and status code",
    ["method", "route", "status"])
HTTP_LATENCY = Histogram(
    "http_request_duration_seconds", "HTTP request latency in seconds", ["method", "route"],
    buckets=(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5))
INFERENCE_LATENCY = Histogram(
    "model_inference_duration_seconds", "Preprocessing + ONNX inference time in seconds",
    buckets=(0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2))
PREDICTIONS = Counter(
    "predictions_total", "Predictions by top plant and condition status", ["plant", "condition_status"])
MODEL_INFO = Gauge(
    "model_info", "Loaded model (value is always 1)", ["version", "architecture", "num_classes"])
MODEL_READY = Gauge("model_ready", "1 if the model is loaded and serving")
