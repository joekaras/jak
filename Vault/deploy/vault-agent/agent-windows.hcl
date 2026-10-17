# Vault Agent / Vault Proxy for one Windows server. Same design as agent-linux.hcl.
#
# Install as a service (elevated). HashiCorp documents running the agent under sc.exe; UNVERIFIED for
# your version, check the docs:
#   sc.exe create VaultProxy binPath= "C:\vault\vault.exe proxy -config=C:\vault\agent-windows.hcl" start= auto
# Lock C:\vault\secrets down to the service account (icacls /inheritance:r, grant only that account and SYSTEM).
#
# Unlike agent-linux.hcl, this uses a persistent, unwrapped secret_id so the proxy survives reboots and
# restarts without a fresh delivery. Compensating controls: the agent role (approle-setup.sh, AGENT_ROLE) binds
# the secret_id and token to the server CIDRs, the policy reads one path only, and secret_id_ttl forces rotation.
# The key custodian places the file once per rotation; never script the value.

pid_file = "C:\\vault\\run\\pid"

vault {
  address = "https://REPLACE-vault-host:8200"
  ca_cert = "C:\\vault\\REPLACE-internal-ca.pem"
}

auto_auth {
  method "approle" {
    namespace = "REPLACE-namespace"
    config = {
      role_id_file_path                   = "C:\\vault\\role-id"
      secret_id_file_path                 = "C:\\vault\\secrets\\secret-id"
      remove_secret_id_file_after_reading = false
    }
  }
}

api_proxy {
  use_auto_auth_token = "force"
}

listener "tcp" {
  address                = "127.0.0.1:8100"
  tls_disable            = true
  require_request_header = true
}
