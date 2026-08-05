#!/usr/bin/env bash
# OSRM icin İzmir/Manisa bolge grafini hazirlar (osrm-extract + partition + customize).
# Kaynak: infra/osrm/izmir-manisa.osm.pbf (repo'da commitli, kucuk bolge extract'i).
# Cikti: osrm-data/ (gitignore'da, docker-compose'daki osrm servisi buradan okur).
#
# Kullanim: ./scripts/prepare-osrm-data.sh
# Her gelistirici bunu bir kere calistirir; docker compose up sonrasinda
# localhost:5555 uzerinden gercek yol mesafesi/rota geometrisi servis edilir.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_PBF="$ROOT_DIR/infra/osrm/izmir-manisa.osm.pbf"
OUT_DIR="$ROOT_DIR/osrm-data"
OSRM_IMAGE="ghcr.io/project-osrm/osrm-backend"

if [ ! -f "$SOURCE_PBF" ]; then
  echo "Kaynak dosya bulunamadi: $SOURCE_PBF" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
cp "$SOURCE_PBF" "$OUT_DIR/izmir-manisa.osm.pbf"

echo "==> osrm-extract"
docker run --rm -t -v "$OUT_DIR:/data" "$OSRM_IMAGE" \
  osrm-extract -p /opt/car.lua /data/izmir-manisa.osm.pbf

echo "==> osrm-partition"
docker run --rm -t -v "$OUT_DIR:/data" "$OSRM_IMAGE" \
  osrm-partition /data/izmir-manisa.osrm

echo "==> osrm-customize"
docker run --rm -t -v "$OUT_DIR:/data" "$OSRM_IMAGE" \
  osrm-customize /data/izmir-manisa.osrm

echo "Tamamlandi. 'docker compose up -d osrm' ile servis localhost:5555 uzerinden ayaga kalkar."
