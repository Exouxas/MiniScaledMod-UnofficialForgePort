# Copilot Instructions

## Project Overview

This is an unofficial Forge port of the [MiniScaled](https://github.com/iPortalTeam/MiniScaledMod) Minecraft mod (originally built for Fabric/Immersive Portals). It targets **Minecraft 1.20.1** with **Forge 1.20.1-47.2.0**.

## Building

To build the mod, run the following command from the root of the repository:

```bat
./gradlew.bat build
```

The output JAR will be placed in `build/libs/`.

### Build Notes

- By default, compile-only API stubs are used for Immersive Portals dependencies (no external artifacts required).
- To build against the real Immersive Portals Forge artifacts (must be available in `mavenLocal` or another configured Maven repo), pass `-PuseRealImmersivePortalsDeps=true`:

```bat
./gradlew.bat build -PuseRealImmersivePortalsDeps=true
```
