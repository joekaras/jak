# Vault Agent / Vault Proxy for one Linux server. Apps on the box call http://127.0.0.1:8100 and send no
# token; the agent adds its own (use_auto_auth_token = "force").
#
# Run with `vault proxy -config=...` on Vault 1.14+ (the API proxy moved from Agent to Proxy), or
# `vault agent -config=...` on older versions, where the api_proxy block is `cache { use_auto_auth_token = "force" }`.
# UNVERIFIED against your Vault version; check the docs for the version you run.
#
# Secret zero still exists for the agent itself: its wrapped secret_id. The gain is that it lives in one
# place per server, outside every app process, and app code holds no credential.

pid_file = "/run/vault-agent/pid"

vault {
  address = "https://REPLACE-vault-host:8200"
  ca_cert = "/etc/pki/ca-trust/source/anchors/REPLACE-internal-ca.pem"
}

auto_auth {
  method "approle" {
    namespace = "REPLACE-namespace"
    config = {
      role_id_file_path                   = "/etc/vault-agent/role-id"
      secret_id_file_path                 = "/run/vault-agent/wrapped-secret-id"
      remove_secret_id_file_after_reading = true
      # The file holds a wrapping token; the agent unwraps it and checks it was minted for this path.
      secret_id_response_wrapping_path    = "auth/approle/role/REPLACE-agent-role/secret-id"
    }
  }
}

api_proxy {
  use_auto_auth_token = "force"
}

listener "tcp" {
  address     = "127.0.0.1:8100"
  tls_disable = true
  # Rejects requests without X-Vault-Request: true (the client always sends it); blunts SSRF.
  require_request_header = true
}
