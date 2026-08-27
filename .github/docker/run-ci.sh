#!/usr/bin/env bash
set -euo pipefail

STAGE="${1:-test}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMPOSE_FILE="$(dirname "$0")/docker-compose.yml"

cd "$ROOT"

case "$STAGE" in
  test)
    docker compose -f "$COMPOSE_FILE" run --rm --build ci-test
    ;;
  package)
    docker compose -f "$COMPOSE_FILE" --profile package run --rm --build ci-package
    ;;
  all)
    docker compose -f "$COMPOSE_FILE" run --rm --build ci-test
    docker compose -f "$COMPOSE_FILE" --profile package run --rm ci-package
    ;;
  *)
    echo "Usage: $0 [test|package|all]" >&2
    exit 1
    ;;
esac
