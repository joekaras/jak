# ibsmas-vault

A Java 8 client for HashiCorp Vault with no runtime dependencies. It reads KV v2 secrets and keys, and encrypts,
decrypts and rewraps through the Transit engine. It authenticates in one of two ways:

- **AppRole with a response-wrapped secret_id**, delivered at deploy time.
- **Through a local Vault Proxy or Agent**, which holds the credential so the app holds none.

```xml
<dependency>
  <groupId>ibsmas</groupId>
  <artifactId>ibsmas-vault</artifactId>
  <version>0.1.0</version>
</dependency>
```

```java
try (VaultSecretClient vault = new VaultSecretClient(VaultConfig.fromEnvironment())) {
    String user = vault.getSecret("ibsmas/orders/snowflake", "user");
    String ciphertext = vault.encrypt("ibsmas-orders", plaintextBytes);
}
```

## Documentation

| Doc | Read it when you need to... |
|---|---|
| [Using the client](docs/01-using-the-client.md) | Add the library to an app: configuration, auth modes, every API call, errors, caching, key handling |
| [Samples](docs/02-samples.md) | Build, configure and run the Linux, Windows-service and Tomcat samples |
| [Local Vault for development](docs/03-local-vault.md) | Run the integration tests against a throwaway Vault, with Docker or without it |
| [On-prem Docker Vault](docs/04-on-prem-docker-vault.md) | Stand up a persistent, TLS-enabled Vault on an on-prem Docker host and configure a tier |
| [Environments and production](docs/05-environments-and-production.md) | Lay out the UNIT, OPAC, OPIN and OPER tiers in Vault, and operate it in production |
| [Open questions](docs/06-open-questions.md) | Decide how secret_ids are delivered, and whether and how to run Vault Proxy |

## Layout

```
ibsmas-vault/
  docs/                  this documentation
  java/
    pom.xml              parent POM (ibsmas:vault-parent)
    ibsmas-vault/        the library (ibsmas:ibsmas-vault)
    poc-common/          shared sample check (PocCheck) and daemon loop (PocDaemon)
    poc-linux/           systemd sample, plus service/vault-poc-java.service
    poc-windows/         Windows-service sample (WinSW), plus service/vault-poc.xml
    poc-tomcat/          Tomcat 9 WAR sample
    it/                  scripts that run the integration tests against a throwaway local Vault
```

## Build

From `ibsmas-vault/java`, with JDK 8 or later and Maven 3:

```sh
mvn install                                          # build, unit-test, install to ~/.m2
mvn -pl ibsmas-vault test -Dtest=VaultConfigTest     # one test class
bash it/run.sh                                       # integration tests against a local Vault
```
