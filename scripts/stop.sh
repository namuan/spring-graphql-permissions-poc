#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SESSION="${TMUX_SESSION_NAME:-graphql-security-poc}"
COMPOSE_PROJECT="${PODMAN_COMPOSE_PROJECT:-graphql-security-poc}"
COMPOSE=(podman compose -f "$ROOT/compose.yaml" -p "$COMPOSE_PROJECT")

if command -v tmux >/dev/null 2>&1 && tmux has-session -t "$SESSION" 2>/dev/null; then
    tmux kill-session -t "$SESSION"
fi

if ! command -v podman >/dev/null 2>&1; then
    exit 0
fi

if podman info >/dev/null 2>&1; then
    "${COMPOSE[@]}" down
else
    printf 'Podman is not running; skipped stopping containers.\n' >&2
fi
