#!/usr/bin/env bash
# Stops the throwaway Vault and Proxy (containers or local processes) and deletes the generated files.
source "$(dirname "$0")/lib.sh"
if command -v docker >/dev/null 2>&1; then
  docker rm -f "$PROXY" "$SERVER" >/dev/null 2>&1
  docker network rm "$NET" >/dev/null 2>&1
fi
stop_bg proxy
stop_bg server
rm -rf "$WORK"
echo "torn down"
