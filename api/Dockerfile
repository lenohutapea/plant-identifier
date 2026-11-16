# syntax=docker/dockerfile:1

# ---- Build stage: install dependencies into a virtualenv ----
FROM python:3.15-slim AS builder
ENV PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1
WORKDIR /build
COPY requirements.txt .
RUN python -m venv /opt/venv \
 && /opt/venv/bin/pip install --upgrade pip \
 && /opt/venv/bin/pip install -r requirements.txt

# ---- Runtime stage: code only, non-root user ----
FROM python:3.15-slim
LABEL org.opencontainers.image.title="plant-identifier-api" \
      org.opencontainers.image.description="Crop and leaf condition classifier (FastAPI + ONNX Runtime)" \
      org.opencontainers.image.source="https://github.com/lenohutapea/plant-identifier" \
      org.opencontainers.image.licenses="CC-BY-SA-3.0"

ENV PATH="/opt/venv/bin:${PATH}" \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    MODEL_DIR=/app/models

RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=builder /opt/venv /opt/venv
COPY app ./app
# The model is not baked into the image: it is mounted (docker -v) or downloaded by an
# init container (Kubernetes) into /app/models, so a new model does not need a new image.
RUN mkdir -p /app/models && chown app:app /app/models

USER 10001:10001
EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD ["python", "-c", "import sys, urllib.request; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/healthz', timeout=2).status == 200 else 1)"]

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--proxy-headers"]
