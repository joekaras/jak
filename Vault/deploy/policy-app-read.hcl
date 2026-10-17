# Read-only policy for one application. Attach it to the app's AppRole role (or to the Vault Agent role
# on that server). Replace REPLACE-kv-mount and REPLACE-app.
#
# KV v2 reads go through <mount>/data/<path>; nothing here allows writing, listing, or reading metadata.

path "REPLACE-kv-mount/data/REPLACE-app/*" {
  capabilities = ["read"]
}

# lookup-self and renew-self come with Vault's built-in "default" policy. Keep "default" attached
# (token_no_default_policy=false): the client uses lookup-self to tell a dead token from a policy denial.
