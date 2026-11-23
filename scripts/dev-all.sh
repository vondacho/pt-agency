#!/usr/bin/env bash
# Starts the participant booking stack in dev mode, each service with its own Dev Services database:
#   pta-customer :8082   pta-shopping :8083   pta-booking :8080   pta-www :8081 (+ Keycloak :8180)
# Logs go to <module>/target/dev.log; Ctrl-C stops everything.
#
# Needs Docker (Dev Services). With Rancher Desktop, if /var/run/docker.sock is not available:
#   DOCKER_HOST=unix://$HOME/.rd/docker.sock scripts/dev-all.sh
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
pids=()

stop() {
  echo "Stopping..."
  for pid in "${pids[@]}"; do kill "$pid" 2>/dev/null || true; done
  wait 2>/dev/null || true
}
trap stop EXIT INT TERM

start() {
  local module=$1 debug=$2
  mkdir -p "$root/$module/target"
  echo "Starting $module (debug port $debug), log: $module/target/dev.log"
  (cd "$root/$module" && exec mvn -B quarkus:dev -Ddebug="$debug" -Dquarkus.console.enabled=false \
      > "$root/$module/target/dev.log" 2>&1) &
  pids+=($!)
}

wait_for() {
  local name=$1 url=$2
  if curl -s -o /dev/null --retry 120 --retry-delay 2 --retry-connrefused --retry-all-errors "$url"; then
    echo "$name is up: $url"
  else
    echo "$name did not start, see its target/dev.log" >&2
    exit 1
  fi
}

# pta-booking needs pta-customer and pta-shopping; pta-www needs pta-booking
start pta-customer 5006
start pta-shopping 5007
start pta-booking 5005
start pta-www 5008

# readiness probed on seeded records (health endpoints live on the management ports)
wait_for pta-customer http://localhost:8082/customers/20000000-0000-0000-0000-00000000000a
wait_for pta-shopping http://localhost:8083/articles/50000000-0000-0000-0000-000000000001
wait_for pta-booking http://localhost:8080/sessions
wait_for pta-www http://localhost:8081/api/me

echo
echo "Open http://localhost:8081 and sign in as alice (has a pass), bob (no subscription) or carol (account not linked)."
echo "Test credentials: pta-www/src/main/resources/keycloak/pta-realm.json"
wait
