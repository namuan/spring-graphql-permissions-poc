#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SESSION="${TMUX_SESSION_NAME:-graphql-security-poc}"
COMPOSE_PROJECT="${PODMAN_COMPOSE_PROJECT:-graphql-security-poc}"
COMPOSE=(podman compose -f "$ROOT/compose.yaml" -p "$COMPOSE_PROJECT")

fail() {
    printf 'Error: %s\n' "$1" >&2
    exit 1
}

command -v podman >/dev/null 2>&1 || fail 'Podman is required.'
command -v tmux >/dev/null 2>&1 || fail 'tmux is required.'

if ! podman info >/dev/null 2>&1; then
    if podman machine exists >/dev/null 2>&1; then
        podman machine start
    else
        fail 'Podman is not running and no Podman machine exists. Run podman machine init or start the configured Podman connection.'
    fi
fi

if tmux has-session -t "$SESSION" 2>/dev/null; then
    printf 'tmux session %s already exists.\n' "$SESSION"
    exec tmux attach-session -t "$SESSION"
fi

if [[ ! -d "$ROOT/frontend/node_modules" ]]; then
    npm install --prefix "$ROOT/frontend"
fi

"${COMPOSE[@]}" up -d

postgres_id="$(podman ps --filter "name=${COMPOSE_PROJECT}_postgres" --format '{{.ID}}' | head -n 1)"
for attempt in {1..30}; do
    if [[ -n "$postgres_id" ]] && podman exec "$postgres_id" pg_isready -U securitypoc -d securitypoc >/dev/null 2>&1; then
        break
    fi
    if [[ "$attempt" == 30 ]]; then
        fail 'PostgreSQL did not become ready. Check the infrastructure pane or podman compose logs.'
    fi
    sleep 1
done

printf -v compose_command '%q ' "${COMPOSE[@]}"
if ! tmux new-session -d -s "$SESSION" -n app -c "$ROOT/backend" './gradlew bootRun'; then
    fail "Could not create tmux session $SESSION."
fi

tmux split-window -h -t "$SESSION:app.0" -c "$ROOT/frontend" 'npm run dev'
tmux split-window -v -t "$SESSION:app.0" -c "$ROOT" "${compose_command}logs -f"
tmux split-window -h -t "$SESSION:app.1" -c "$ROOT" 'bash'
tmux select-layout -t "$SESSION:app" tiled
printf 'Started %s. Attach with tmux attach -t %s.\n' "$SESSION" "$SESSION"
if [[ -t 0 && -t 1 ]]; then
    exec tmux attach-session -t "$SESSION"
fi
