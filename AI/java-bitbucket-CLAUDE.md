# Project instructions: Java / Python repositories (save as CLAUDE.md at the repo root)

Lines marked TODO are placeholders. Fill them in or delete them; do not leave guesses.

## Stack
- Java 8 (OpenJDK / Temurin), built with Maven. No language or library features newer than Java 8: no `var`, records, text blocks, switch expressions, `List.of` / `Map.of`, or `Optional` methods added after 8.
- TODO: Python version and how dependencies are managed (`requirements.txt`, `pyproject.toml`).
- Use the dependency versions already in `pom.xml`. Ask before adding a dependency or changing a version.

## Source control (Git on Bitbucket)
- Do not commit, push, create branches, or open pull requests. Leave changes unstaged for me.
- TODO: branch naming and pull request conventions, if you want them followed in drafts.

## Build and test
- TODO: build command (for example the Maven goal and any required profile or settings file).
- TODO: test command, and how to run a single test class.
- CI runs on Jenkins. The `Jenkinsfile` is the authority on how the build actually runs; read it before proposing build changes, and do not edit it without asking.

## Rules that exist for compliance reasons
- XML parsing: disable DTDs and external entities on every parser factory (`disallow-doctype-decl`). This applies to JAXB unmarshalling as well; feed it a hardened `SAXParserFactory` source.
- Cryptography: AES-256 through the JCA providers already in use. No custom cryptographic code, no hard-coded keys or IVs.
- Monetary values: use `BigDecimal` with an explicit rounding mode. Do not change rounding or currency calculation logic as a side effect of another change. If a change touches such code, stop and tell me.
- Do not log secrets, keys, tokens, or account data.

## Security scanning
- Checkmarx One scans this code. Fix findings in code. Do not add scan exclusions, suppressions, or preset changes.
- Do not run `cx` scans or download scanner tooling.

## Modules
- TODO: one line per Maven module: path, what it does, who owns it.
