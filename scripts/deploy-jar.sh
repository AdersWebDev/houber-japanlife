#!/bin/bash
# Replace the running app with a new JAR: backup -> stop -> swap -> start -> health check.
# If the new version does not come up, it rolls back to the backup automatically.
# The app is restarted with the same command line it is running with now,
# so the Jasypt password is reused without being typed or printed.
#
# Usage (on the server, as ec2-user, without sudo):
#   bash ~/deploy-jar.sh [new jar path]      (default: ~/osaka-city-0.0.1-SNAPSHOT.jar)
#   bash ~/deploy-jar.sh --check [new jar]   (only run the checks, change nothing)
set -u

APP_DIR=${APP_DIR:-/home/ec2-user/japanlife}
JAR_NAME=osaka-city-0.0.1-SNAPSHOT.jar
APP_JAR=$APP_DIR/$JAR_NAME
LOG=$APP_DIR/nohup.out
PORT=${PORT:-40002}
CHECK_ONLY=0
if [ "${1:-}" = "--check" ]; then CHECK_ONLY=1; shift; fi
NEW_JAR=${1:-$HOME/$JAR_NAME}

say()  { echo "[$(date +%H:%M:%S)] $*"; }
fail() { say "ERROR: $*"; exit 1; }

# PIDs of the java processes running this app's jar
app_pids() {
  local p
  for p in $(pgrep -x java); do
    tr '\0' ' ' < /proc/$p/cmdline 2>/dev/null | grep -qF "$JAR_NAME" && echo "$p"
  done
}

# true if something accepts connections on $PORT
port_busy() { (exec 3<>/dev/tcp/127.0.0.1/$PORT) 2>/dev/null; }

stop_app() {
  local pids i
  pids=$(app_pids)
  [ -z "$pids" ] && return 0
  say "stopping app (pid $(echo $pids))"
  kill -15 $pids
  for i in $(seq 1 30); do
    [ -z "$(app_pids)" ] && return 0
    sleep 1
  done
  say "still running after 30s -> kill -9"
  kill -9 $(app_pids) 2>/dev/null
  sleep 2
}

start_app() {
  [ -f "$LOG" ] && mv -f "$LOG" "$LOG.prev"
  (cd "$RUN_DIR" || exit 1; exec nohup "${ARGS[@]}" > "$LOG" 2>&1 < /dev/null) &
}

http_code() { curl -s -o /dev/null -w '%{http_code}' -m "$2" "http://localhost:$PORT$1"; }

# up = static file served (Spring context incl. DB started), then the home page must render
wait_healthy() {
  local i
  for i in $(seq 1 60); do
    sleep 3
    [ "$(http_code /robots.txt 5)" = "200" ] && break
    [ -z "$(app_pids)" ] && return 1
    [ "$i" = 60 ] && return 1
  done
  for i in 1 2 3; do
    [ "$(http_code / 30)" = "200" ] && return 0
    sleep 3
  done
  return 1
}

# 0) checks
[ -f "$NEW_JAR" ] || fail "new jar not found: $NEW_JAR"
if command -v unzip >/dev/null; then LIST="unzip -l"; elif command -v jar >/dev/null; then LIST="jar tf"; else LIST=""; fi
if [ -n "$LIST" ]; then
  $LIST "$NEW_JAR" 2>/dev/null | grep -q 'BOOT-INF/classes/application.yml' \
    || fail "not a Spring Boot jar of this app (upload the .jar, not the .zip): $NEW_JAR"
fi
[ -f "$APP_JAR" ] || fail "current jar not found: $APP_JAR"
[ "$(id -un)" = "$(stat -c %U "$APP_JAR")" ] || fail "run as $(stat -c %U "$APP_JAR") without sudo"
FREE_KB=$(df -Pk "$APP_DIR" | awk 'NR==2 {print $4}')
[ "${FREE_KB:-0}" -gt 307200 ] || fail "less than 300MB of free disk space. nothing was changed."

# 1) reuse the current start command
ARGS=()
PID=$(app_pids | head -1)
if [ -n "$PID" ]; then
  while IFS= read -r -d '' a; do ARGS+=("$a"); done < /proc/$PID/cmdline
  [ ${#ARGS[@]} -gt 0 ] || fail "could not read the start command of pid $PID. nothing was changed."
  if printf '%s\n' "${ARGS[@]}" | grep -q 'jasypt.encryptor.password'; then
    PW_FROM="command line"
  else
    ENVPW=$(tr '\0' '\n' < /proc/$PID/environ 2>/dev/null | grep '^JASYPT_ENCRYPTOR_PASSWORD=' | head -1)
    [ -n "$ENVPW" ] || fail "could not find how the Jasypt password is passed to the app. nothing was changed."
    export "$ENVPW"
    PW_FROM="environment variable"
  fi
  RUN_DIR=$(readlink /proc/$PID/cwd)
  say "running app: pid $PID (Jasypt password found in its $PW_FROM)"
elif [ "$CHECK_ONLY" = 1 ]; then
  port_busy && fail "port $PORT is in use, but the app process was not found. nothing was changed."
  say "app is not running. a real deploy will ask for the Jasypt password."
else
  port_busy && fail "port $PORT is in use, but the app process was not found. nothing was changed."
  say "app is not running. enter the Jasypt password to start it."
  read -r -s -p "Jasypt password: " PW; echo
  [ -n "$PW" ] || fail "password is empty"
  ARGS=(java -jar "-Djasypt.encryptor.password=$PW" "$APP_JAR")
  RUN_DIR=$APP_DIR
fi

if [ "$CHECK_ONLY" = 1 ]; then
  say "new jar: $NEW_JAR ($(du -h "$NEW_JAR" | cut -f1))"
  say "free disk: $((FREE_KB / 1024))MB"
  say "CHECK OK: ready to deploy. nothing was changed."
  exit 0
fi

# 2) backup -> stop -> swap -> start
BACKUP="$APP_JAR.bak-$(date +%Y%m%d-%H%M%S)"
cp -p "$APP_JAR" "$BACKUP" || fail "backup failed"
say "backup: $BACKUP"
stop_app
[ -z "$(app_pids)" ] || fail "could not stop the running app"
for i in $(seq 1 15); do port_busy || break; sleep 1; done
port_busy && fail "port $PORT is still in use after stopping the app (check: ss -tlnp | grep $PORT)"
if ! mv -f "$NEW_JAR" "$APP_JAR"; then
  say "swap failed -> starting the current version again"
  start_app
  exit 1
fi
say "starting new version (takes up to a few minutes)..."
start_app
if wait_healthy; then
  say "OK: new version is up"
  say "log: $LOG"
  exit 0
fi

# 3) rollback
say "new version did not come up. last log lines:"
tail -n 30 "$LOG"
cp -f "$LOG" "$LOG.failed"
stop_app
for i in $(seq 1 15); do port_busy || break; sleep 1; done
cp -p "$BACKUP" "$APP_JAR"
say "rolling back to $BACKUP ..."
start_app
if wait_healthy; then
  say "rolled back: previous version is up again. failed log: $LOG.failed"
else
  say "ROLLBACK ALSO FAILED - check $LOG"
fi
exit 1
