# Copilot Instructions

## Project Overview

This is an unofficial Forge port of the [MiniScaled](https://github.com/iPortalTeam/MiniScaledMod) Minecraft mod (originally built for Fabric/Immersive Portals). It targets **Minecraft 1.20.1** with **Forge 1.20.1-47.2.0**.

## Building

The project uses the Gradle wrapper (`gradlew.bat` on Windows). The terminal environment is a **Git Bash shell on Windows**, where `./gradlew.bat` does not work reliably. Always invoke builds using the bash wrapper instead:

```bash
bash gradlew build
```

To compile only (faster, no JAR):

```bash
bash gradlew compileJava
```

The output JAR will be placed in `build/libs/`.

### Build Notes

- By default, compile-only API stubs are used for Immersive Portals dependencies (no external artifacts required).
- To build against the real Immersive Portals Forge artifacts (must be available in `mavenLocal` or another configured Maven repo), pass `-PuseRealImmersivePortalsDeps=true`:

```bash
bash gradlew build -PuseRealImmersivePortalsDeps=true
```

### Terminal Tips

- The working directory must be the repo root (`C:\DEV\MiniScaledMod-UnofficialForgePort`) when running Gradle commands.
- Do **not** use `./gradlew.bat`, `gradlew.bat` (without `./`), or `./gradlew` directly — they fail in Git Bash on this machine. Use `bash gradlew <task>`.
- `BUILD SUCCESSFUL` in the output confirms success. The exit code is also reliable: `echo $?` after the command prints `0` on success.
