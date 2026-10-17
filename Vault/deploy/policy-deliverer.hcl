# Policy for the deploy pipeline's own Vault identity (JWT/OIDC from Jenkins or Azure DevOps, or a
# separate AppRole). It can mint secret_ids for one app role, and only response-wrapped ones:
# Vault refuses the request unless the caller asks for a wrap TTL inside these bounds.

path "auth/approle/role/REPLACE-role/secret-id" {
  capabilities     = ["update"]
  min_wrapping_ttl = "1m"
  max_wrapping_ttl = "15m"
}
