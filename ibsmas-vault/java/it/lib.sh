# Shared by the it/ scripts; source it, don't run it.
# Mode: IT_MODE=docker | native | auto (default). auto uses Docker when the engine answers, otherwise a local
# vault binary (VAULT_BIN, or `vault` on PATH). setup.sh records the choice in .work/mode for the other scripts.
# Git Bash rewrites /unix/style arguments into Windows paths. docker, vault, and taskkill calls turn that off with
# MSYS_NO_PATHCONV=1 one command at a time; exporting it would break mvn, whose launcher relies on the rewrite.

IT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK="$IT_DIR/.work"
# One Vault version for both modes, so Docker and native runs test the same server. IT_VAULT_VERSION overrides it.
VAULT_VERSION="${IT_VAULT_VERSION:-1.18.3}"
IMAGE="hashicorp/vault:$VAULT_VERSION"
NET=ibsmas-vault-it-net
SERVER=ibsmas-vault-it
AGENT=ibsmas-vault-it-agent

# A path a Windows program can read. cygpath -m gives C:/..., which HCL and Java accept without escaping.
hostpath() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi; }

vault_bin() { printf '%s' "${VAULT_BIN:-vault}"; }

it_mode() {
  if [ -f "$WORK/mode" ]; then cat "$WORK/mode"; return; fi
  case "${IT_MODE:-auto}" in
    docker|native) printf '%s' "$IT_MODE" ;;
    auto) if docker info >/dev/null 2>&1; then printf docker; else printf native; fi ;;
    *) echo "IT_MODE must be docker, native, or auto" >&2; return 1 ;;
  esac
}

# Runs the vault CLI with the dev server's root token.
v() {
  local root
  root="$(cat "$WORK/root-token")"
  if [ "$(it_mode)" = docker ]; then
    MSYS_NO_PATHCONV=1 docker exec -i -e VAULT_ADDR=http://127.0.0.1:8200 -e VAULT_TOKEN="$root" "$SERVER" vault "$@"
  else
    MSYS_NO_PATHCONV=1 VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN="$root" "$(vault_bin)" "$@"
  fi
}

# Native mode: start a background process with its output in .work/<name>.log, and remember its PID.
# Under Git Bash the Windows PID is kept too, because taskkill is what reliably stops vault.exe.
start_bg() {
  local name="$1"
  shift
  MSYS_NO_PATHCONV=1 "$@" </dev/null >"$WORK/$name.log" 2>&1 &
  local pid=$!
  echo "$pid" > "$WORK/$name.pid"
  if [ -r "/proc/$pid/winpid" ]; then cat "/proc/$pid/winpid" > "$WORK/$name.winpid"; fi
}

stop_bg() {
  local name="$1"
  if [ -f "$WORK/$name.winpid" ] && command -v taskkill >/dev/null 2>&1; then
    MSYS_NO_PATHCONV=1 taskkill /F /PID "$(cat "$WORK/$name.winpid")" >/dev/null 2>&1 || true
  elif [ -f "$WORK/$name.pid" ]; then
    kill "$(cat "$WORK/$name.pid")" 2>/dev/null || true
  fi
}
