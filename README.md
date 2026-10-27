# jak

Home of **ibsmas-vault**, a dependency-free Java 8 client for HashiCorp Vault:
- reads KV v2 secrets and keys, including specific versions;
- encrypts, decrypts and rewraps through the Transit engine;
- authenticates with AppRole using a response-wrapped secret_id, or through a local Vault Proxy.

**Start here:** [ibsmas-vault/README.md](ibsmas-vault/README.md) has the overview, a quick example and the index of
the developer docs.

## Quick start

```sh
cd ibsmas-vault/java
mvn install          # build, run unit tests, install ibsmas:ibsmas-vault:0.1.0 to ~/.m2
bash it/run.sh       # integration tests against a throwaway local Vault 1.18.3 (Docker, or a local vault binary)
```

Requirements:
- JDK 8 or later and Maven 3;
- for `it/run.sh`: Docker, or a `vault` 1.18.3 binary (`winget install Hashicorp.Vault --version 1.18.3`), plus
  Git Bash on Windows.

## Docs

| | |
|---|---|
| [Using the client](ibsmas-vault/docs/01-using-the-client.md) | Configuration, auth modes, KV, Transit, errors |
| [Samples](ibsmas-vault/docs/02-samples.md) | Linux service, Windows service, Tomcat |
| [Local Vault](ibsmas-vault/docs/03-local-vault.md) | Running the integration tests locally |
| [On-prem Docker Vault](ibsmas-vault/docs/04-on-prem-docker-vault.md) | A persistent, TLS Vault on an on-prem Docker host |
| [Environments and production](ibsmas-vault/docs/05-environments-and-production.md) | UNIT, OPAC, OPIN, OPER tiers, and running Vault in production |
| [Open questions](ibsmas-vault/docs/06-open-questions.md) | secret_id delivery and Vault Proxy decisions |

## Repository layout

```
ibsmas-vault/
  README.md        library overview
  docs/            developer documentation
  java/            Maven build: the library, samples, and it/ integration-test scripts
CLAUDE.md          notes for Claude Code
```
