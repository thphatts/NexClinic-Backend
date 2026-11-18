#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/nexclinic}"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.ec2.yml"
RELEASE_ENV="$DEPLOY_DIR/release.env"
IMAGE="${1:?Usage: ec2-deploy.sh ghcr.io/owner/image@sha256:digest}"
[[ "$IMAGE" =~ ^ghcr\.io/[a-z0-9._/-]+@sha256:[a-f0-9]{64}$ ]] || { echo 'Invalid image digest' >&2; exit 1; }
[[ -f "$DEPLOY_DIR/.env" ]] || { echo "Missing $DEPLOY_DIR/.env" >&2; exit 1; }
exec 9>"$DEPLOY_DIR/deploy.lock"
flock -n 9 || { echo 'Another deployment is running' >&2; exit 1; }

compose() {
  docker compose --project-name nexclinic --env-file "$DEPLOY_DIR/.env" --env-file "$RELEASE_ENV" -f "$COMPOSE_FILE" "$@"
}

# First deployment has no previous release file.
[[ -f "$RELEASE_ENV" ]] || printf 'IMAGE=%s\n' "$IMAGE" > "$RELEASE_ENV"
container=$(compose ps -q app)
CURRENT_IMAGE=''
if [[ -n "$container" ]]; then
  CURRENT_IMAGE=$(docker inspect --format '{{.Config.Image}}' "$container")
fi

healthy() {
  local container state attempt
  for ((attempt=0; attempt<60; attempt++)); do
    container=$(compose ps -q app)
    state=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container" 2>/dev/null || true)
    [[ "$state" != healthy ]] || return 0
    sleep 2
  done
  return 1
}

rollback() {
  local result=$?
  trap - ERR
  echo 'Release failed; restoring previous container image.' >&2
  compose logs --tail=100 app >&2 || true
  if [[ -n "$CURRENT_IMAGE" && "$CURRENT_IMAGE" != "$IMAGE" ]]; then
    printf 'IMAGE=%s\n' "$CURRENT_IMAGE" > "$RELEASE_ENV"
    if compose up -d --no-deps app && healthy; then
      echo 'Previous image restored and healthy.' >&2
    else
      echo 'ROLLBACK FAILED: operator action required.' >&2
    fi
  else
    echo 'No previous image available for rollback.' >&2
  fi
  exit "$result"
}

printf 'IMAGE=%s\n' "$IMAGE" > "$RELEASE_ENV"
trap rollback ERR
compose config --quiet
compose pull app
compose up -d --wait --wait-timeout 180 postgres_db redis app
healthy
trap - ERR
printf 'Deployed %s\n' "$IMAGE"
