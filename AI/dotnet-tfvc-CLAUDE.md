# Project instructions: .NET / C++ monorepo (save as CLAUDE.md at the workspace root)

Lines marked TODO are placeholders. Fill them in or delete them; do not leave guesses.

## Stack
- C# on .NET Framework 4.8. Use only language features that the projects' configured `LangVersion` allows (default for 4.8 is C# 7.3). No APIs that exist only in .NET Core / .NET 5+.
- C++14 on MSVC. No C++17 or later features (`std::optional`, `std::string_view`, structured bindings, `if constexpr`). Check the project's platform toolset in its `.vcxproj` before using a library feature.
- Win32 and COM throughout. Legacy code uses raw interface pointers and manual `AddRef`/`Release`; match the ownership style of the file you are editing.

## Source control (TFVC on Azure DevOps)
- This is TFVC, not Git. Do not run `git` commands here.
- Do not check in, shelve, or undo pending changes. Leave edits as pending changes for me.
- TODO: workspace type. If this is a server workspace, files are read-only until checked out; ask me before running `tf checkout`.
- This is a monorepo. Stay inside the component I name. Treat everything else as external and do not explore it.

## Build and test
- TODO: build command for a single solution (for example the `msbuild` invocation and configuration).
- TODO: test command and test framework.
- TODO: where the pipeline definitions live.
- Do not build the whole repository. Build only the solution or project being changed.

## Rules that exist for compliance reasons
- Cryptography: AES-256 through Windows CNG (BCrypt APIs in C++, CNG classes in C#). No custom cryptographic code, no legacy CryptoAPI, no hard-coded keys or IVs.
- Monetary values: do not change rounding or currency calculation logic as a side effect of another change. If a change touches such code, stop and tell me.
- Do not log secrets, keys, tokens, or account data.

## Security scanning
- Checkmarx One scans this code. Fix findings in code. Do not add scan exclusions, suppressions, or preset changes.
- Do not run `cx` scans or download scanner tooling.

## Components
- TODO: one line per major component: path, what it does, who owns it.
