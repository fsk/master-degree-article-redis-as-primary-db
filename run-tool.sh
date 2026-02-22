#!/usr/bin/env bash
set -euo pipefail

PROJECT="${1:-}"
EXTRA_ARGS=()
if [[ $# -gt 0 ]]; then
  shift
  EXTRA_ARGS=("$@")
fi

if [[ -z "${PROJECT}" ]]; then
  echo "Kullanim: ./run-tool.sh <proje-adi>" >&2
  echo "Ornek:  ./run-tool.sh data-tools generate" >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${ROOT_DIR}/${PROJECT}"

if [[ ! -d "${PROJECT_DIR}" ]]; then
  echo "Proje bulunamadi: ${PROJECT_DIR}" >&2
  exit 1
fi

if [[ ! -f "${PROJECT_DIR}/main.py" ]]; then
  echo "main.py bulunamadi: ${PROJECT_DIR}" >&2
  exit 1
fi

if [[ ! -f "${PROJECT_DIR}/requirements.txt" ]]; then
  echo "requirements.txt bulunamadi: ${PROJECT_DIR}" >&2
  exit 1
fi

cd "${PROJECT_DIR}"

if [[ ! -f ".venv/bin/python" ]]; then
  python -m venv .venv
fi

PYTHON_BIN=".venv/bin/python"
${PYTHON_BIN} -m pip install -r requirements.txt

${PYTHON_BIN} main.py "${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}"
