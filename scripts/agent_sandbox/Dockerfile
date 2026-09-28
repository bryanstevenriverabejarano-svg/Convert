FROM python:3.13-slim-bookworm
RUN apt-get update && apt-get install -y --no-install-recommends chromium && rm -rf /var/lib/apt/lists/*
COPY worker.py /opt/salve/worker.py
USER 65534:65534
WORKDIR /tmp
