## MiniScaled Mod (Unofficial Forge Port)

An unofficial Forge 1.20.1 port of the [MiniScaled Fabric mod](https://github.com/qouteall/MiniScaledMod) that provides easy-usable scale boxes using [Immersive Portals](https://github.com/iPortalTeam/ImmersivePortalsModForNeo) functionality. You can enter the scale box seamlessly without loading screen.

![miniscaled1.png](https://i.loli.net/2021/09/30/J9bBF82tRu5yIkW.png)

### How to use

[Check the Wiki](https://qouteall.fun/immptl/wiki/MiniScaled.html)

### Runtime Requirements

These mods must be present in your `mods/` folder to run:

| Mod | Required Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.2.0+ |
| [Immersive Portals (Forge)](https://github.com/iPortalTeam/ImmersivePortalsModForNeo) | **6.0.x or newer** (versions from qouteall's original 3.x repo are NOT compatible — see note below) |
| [Cloth Config (Forge)](https://github.com/shedaniel/cloth-config) | 11.1.106+ |

> **⚠️ ImmersivePortals version note:** This mod targets [iPortalTeam/ImmersivePortalsModForNeo](https://github.com/iPortalTeam/ImmersivePortalsModForNeo) (version 6.0.x+), which uses a `PortalRenderingPredicateEvent` on the NeoForge/Forge event bus to filter portal rendering. The original qouteall ImmersivePortals 3.x releases (e.g. `immersive-portals-3.0.7-all.jar`) are from a **different repo** and are **not compatible**.

---

### Building

This mod can be built in two modes:

1) **Self-contained build (default)**: uses compile-only API stubs for Immersive Portals + q_misc_util. This is enough for CI-style compilation/jar output, but you still need the real Immersive Portals (Forge) mod to run.
2) **Real dependency build**: uses the actual Immersive Portals (Forge) + q_misc_util artifacts from Maven (typically `mavenLocal`).

**Real dependency build requirements (must be in local Maven, `~/.m2/`):**

| Dependency | Group:Artifact:Version |
|---|---|
| Immersive Portals Core | `qouteall:imm_ptl_core:<version>` (iPortalTeam 6.0.x build) |
| Immersive Portals Misc Utils | `qouteall:q_misc_util:<version>` (iPortalTeam 6.0.x build) |

These must be **iPortalTeam's NeoForge builds** of ImmersivePortals installed into your local Maven repository
(`mvn install` or `./gradlew publishToMavenLocal` from [iPortalTeam/ImmersivePortalsModForNeo](https://github.com/iPortalTeam/ImmersivePortalsModForNeo)).

**Public Maven dependencies** (downloaded automatically):
- Forge 1.20.1-47.2.0+ (from `https://maven.minecraftforge.net`)
- Cloth Config Forge 11.1.106+ (from `https://maven.shedaniel.me/`)

**Build steps:**

```bash
# Option A (default): self-contained build (compiles against stubs)
./gradlew build

# Option B: build against the real ImmersivePortals Forge artifacts
# 1. Install ImmersivePortals Forge to local Maven first (see above)
# 2. Build with the flag enabled
./gradlew build -PuseRealImmersivePortalsDeps=true

# Output JAR is in build/libs/
```

**Run in development:**

```bash
# Run Minecraft client
./gradlew runClient

# Run Minecraft server
./gradlew runServer
```

### Changes from original Fabric mod

This port replaces Fabric-specific APIs with Forge equivalents:

- `@ModInitializer` → `@Mod` constructor with `FMLJavaModLoadingContext`
- `@ClientModInitializer` → `FMLClientSetupEvent` + `EntityRenderersEvent.RegisterRenderers`
- `FabricEntityTypeBuilder` → `EntityType.Builder` via Forge `DeferredRegister`
- `FabricBlockEntityTypeBuilder` → `BlockEntityType.Builder` via Forge `DeferredRegister`
- `UseBlockCallback.EVENT` → `PlayerInteractEvent.RightClickBlock`
- `ClientTickEvents.END_CLIENT_TICK` → `TickEvent.ClientTickEvent`
- `ServerTickEvents.END_SERVER_TICK` → `TickEvent.ServerTickEvent`
- `ItemGroupEvents.modifyEntriesEvent` → `BuildCreativeModeTabContentsEvent`
- `CommandRegistrationCallback` → `RegisterCommandsEvent`
- `ServerLifecycleEvents.SERVER_STARTED` → `ServerStartedEvent`
- `EntityRendererRegistry` → `EntityRenderersEvent.RegisterRenderers`
- `ModMenuApi` (config screen) → `ConfigScreenHandler.ConfigScreenFactory`
- `@Environment(EnvType.CLIENT)` → `@OnlyIn(Dist.CLIENT)`
- All block/item/entity registrations via `DeferredRegister`

