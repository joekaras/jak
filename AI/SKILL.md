---
name: codebase-onboarding
description: Execute a structured 5-phase architectural reconnaissance on an unfamiliar repository or component and write SYSTEM_MAP.md. Use when onboarding to a new codebase or subsystem.
argument-hint: [path-to-repo-or-component]
---

# Codebase Architectural Reconnaissance

Target: `$ARGUMENTS` (if empty, use the current working directory).

## Ground rules

- **Scope.** Inspect only the target path. Anything outside it is external: name it, do not explore it.
- **Evidence.** Every claim in the deliverable carries a `path:line` citation. No general summaries.
- **No guessing.** If something cannot be established from source, write `NOT FOUND` (looked, absent) or `UNVERIFIED` (suspected, no direct evidence). Never fill a gap with a plausible answer.
- **Read-only.** Do not modify, build, or run anything. The only file written is `docs/architecture/SYSTEM_MAP.md` under the target root (create the folder if it does not exist).
- **Context discipline.** For large targets, run each phase in a subagent that returns findings and citations only, not file contents.

## Phase 1: Build & Toolchain Inventory

1. Locate build and configuration files: `.sln`, `.csproj`, `.vcxproj`, `Directory.Build.props`, `packages.config`, `CMakeLists.txt`, `pom.xml`, `build.gradle`, `pyproject.toml`, `requirements.txt`, `package.json`.
2. Locate CI definitions (`Jenkinsfile`, `azure-pipelines.yml`, build scripts). Treat these as the authority on how the code is actually built.
3. Identify target frameworks, platform toolsets, and compiler/language versions.
4. List the dependencies that shape the architecture: application framework, data access, messaging, cryptography, logging. Skip utility libraries.
5. Document the terminal commands to build, run tests, and run lints, citing where each command comes from. Do not execute them.

## Phase 2: Runtime Entry Points

1. Find everything the OS or a host can start or load:
   - Executables: console apps, UI shells, scheduled jobs.
   - Windows services and IIS- or container-hosted web apps.
   - COM servers: IDL files, `DllGetClassObject`, `DllRegisterServer`, registration scripts, CLSID/ProgID declarations.
   - Message or queue consumers and file-drop watchers.
2. For each, trace the boot sequence: how configuration, logging, and object wiring are initialized. Wiring means dependency injection where it exists, otherwise the equivalent: class factories, global singletons, registry lookups, config-driven loading.

## Phase 3: Data Layer & State Invariants

1. Identify all persistence and state: relational databases, caches, local disk files, registry, in-process shared state.
2. Locate schema definitions, migrations, and stored procedures. If the schema lives outside the target, record `NOT FOUND` and whatever the code reveals about who owns it.
3. Document the primary data access pattern (EF, Dapper, PetaPoco, ADO.NET, JDBC, ODBC, raw SQL).
4. Document concurrency handling with citations: transaction boundaries, isolation levels, locking, optimistic-concurrency tokens, retry logic.

## Phase 4: External Boundaries

Identify everything that crosses the edge of the target, in both directions.

1. **Inbound:** exposed APIs (REST, SOAP, gRPC), COM interfaces, exported DLL functions, queues consumed, files or directories watched, scheduled triggers.
2. **Outbound:** HTTP/SOAP clients, databases owned by other systems, queues published to, file drops written, COM objects activated by CLSID or ProgID, P/Invoke and native interop, third-party SDKs.
3. For each boundary record: protocol, payload or contract definition (WSDL, IDL, proto, DTO class), authentication mechanism, and where the endpoint or address is configured.
4. Note where secrets and credentials are read from. Record the location only, never the values.
5. Mark any counterpart system that cannot be identified from source as `UNVERIFIED`.

## Phase 5: Deliverable (SYSTEM_MAP.md)

Write `docs/architecture/SYSTEM_MAP.md` under the target root with these sections.

### 1. Context diagram (required)
The target as a single box, surrounded by the users and external systems from Phase 4. Label each arrow with protocol and direction.

### 2. Container diagram (required)
Inside the target: each separately running or deployable unit from Phase 2 (executables, services, COM servers, web apps) plus each data store from Phase 3. Label arrows with protocol or access pattern.

### 3. Component diagram (optional)
Only for a container whose internal structure is non-obvious and matters to a newcomer. Omit otherwise, and say that it was omitted.

Diagram rules:
- Plain Mermaid `flowchart` with `subgraph` blocks for boundaries. Do not use Mermaid's `C4Context` / `C4Container` diagram types.
- Maximum 15 nodes per diagram. Group or split rather than exceed it.
- Every node and edge must correspond to something cited elsewhere in the document.

### 4. Component Inventory Table
`Path | Responsibility | Inbound Callers | Outbound Dependencies | Evidence`

For inbound callers outside the target, write `unknown outside scope` rather than guessing.

### 5. External Boundaries Table
`Boundary | Direction | Protocol | Contract | Auth | Config Location | Evidence`

### 6. Build & Run
The commands from Phase 1, each with its source citation.

### 7. Critical Invariants & Gotchas
Up to 5 non-obvious rules, thread-safety caveats, or legacy quirks, each with a citation. List fewer if fewer were found. Do not pad.

### 8. Open Questions
Everything marked `NOT FOUND` or `UNVERIFIED`, phrased as a question a teammate could answer.
