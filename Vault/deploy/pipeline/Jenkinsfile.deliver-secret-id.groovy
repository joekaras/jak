// Jenkins stage that delivers a wrapped, single-use secret_id to a Linux app server and restarts the app.
// Illustrative; adapt the host, role, and how the pipeline itself authenticates to Vault.
//
// The pipeline logs in to Vault with its own identity (JWT/OIDC preferred), whose policy is
// deploy/policy-deliverer.hcl: it can mint wrapped secret_ids for one role and nothing else.

stage('Deliver wrapped secret_id') {
    steps {
        sh '''#!/bin/bash
            # Jenkins runs sh steps with -x, which would print the wrapping token into the build log.
            set +x -euo pipefail

            WRAPPED="$(vault write -wrap-ttl=10m -field=wrapping_token -f "auth/approle/role/${VAULT_ROLE}/secret-id")"
            if [ -z "$WRAPPED" ] || [ "$WRAPPED" = "null" ]; then
                echo "Vault did not return a wrapping token" >&2
                exit 1
            fi

            # stdin, not argv: keeps the token out of process listings on both ends.
            printf '%s' "$WRAPPED" | ssh "deploy@${APP_HOST}" \
              "sudo install -o vault-poc -g vault-poc -m 0400 /dev/stdin /run/vault-poc-java/wrapped-secret-id \
               && sudo systemctl restart vault-poc-java"
            unset WRAPPED
        '''
    }
}
