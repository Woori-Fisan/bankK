#!/bin/sh
sed -e "s|PLATFORM_BACKEND_HOST|${PLATFORM_BACKEND_HOST}|g" \
    -e "s|PLATFORM_BACKEND_PORT|${PLATFORM_BACKEND_PORT}|g" \
    /etc/prometheus/prometheus.yml.tmpl > /tmp/prometheus.yml

exec /bin/prometheus \
  --config.file=/tmp/prometheus.yml \
  --storage.tsdb.retention.time=15d