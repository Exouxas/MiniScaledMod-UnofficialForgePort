## MiniScaled Mod (Unofficial Forge Port)

An unofficial Forge 1.20.1 port of the [MiniScaled Fabric mod](https://github.com/qouteall/MiniScaledMod) that provides easy-usable scale boxes using [Immersive Portals](https://github.com/qouteall/ImmersivePortalsMod) functionality. You can enter the scale box seamlessly without loading screen.

![miniscaled1.png](https://i.loli.net/2021/09/30/J9bBF82tRu5yIkW.png)

### How to use

[Check the Wiki](https://qouteall.fun/immptl/wiki/MiniScaled.html)

### Building

This mod can be built in two modes:

1) **Self-contained build (default)**: uses compile-only API stubs for Immersive Portals + q_misc_util. This is enough for CI-style compilation/jar output, but you still need the real Immersive Portals (Forge) mod to run.
2) **Real dependency build**: uses the actual Immersive Portals (Forge) + q_misc_util artifacts from Maven (typically `mavenLocal`).

**Real dependency build requirements (must be in local Maven, `~/.m2/`):**

| Dependency | Group:Artifact:Version |
|---|---|
| Immersive Portals Core (Forge) | `qouteall:imm_ptl_core:3.3.1.7` |
| Immersive Portals Misc Utils (Forge) | `qouteall:q_misc_util:3.3.1.7` |

These must be the **Forge** builds of ImmersivePortals installed into your local Maven repository
(`mvn install` or `./gradlew publishToMavenLocal` from the ImmersivePortals Forge source).

**Public Maven dependencies** (downloaded automatically):
- Forge 1.20.1-47.2.0 (from `https://maven.minecraftforge.net`)
- Cloth Config Forge 11.1.106 (from `https://maven.shedaniel.me/`)

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

