# Minecraft 26.1.x vs 26.2: every difference we have hit

One jar must run on both versions. Mojang ships mojmap names at runtime
(no intermediary since 26.x), so any renamed class, field or method is a
hard `NoSuchFieldError`/`NoSuchMethodError`/mixin-apply failure on the
other version. These are the differences found the hard way, and how each
one is handled. Grow this list with every new find.

## Renames and moves

| What | 26.1.x | 26.2 | Handling |
|---|---|---|---|
| HUD class | `net.minecraft.client.gui.Gui` | `net.minecraft.client.gui.Hud` (members identical) | Mixin pair (`GuiOverlayMixin`/`HudOverlayMixin`) + `ClearSightMixinPlugin` picks one by version string |
| Current screen on `Minecraft` | public field `screen` | method `screen()` (field removed) | Reflection lookup by name, cached (rare-fish-finder) |
| `Minecraft.setScreen` | exists | removed; only `setScreenAndShow` | Always call `setScreenAndShow` (exists in both) |
| Entity type constants | statics on `EntityType` (`EntityType.TROPICAL_FISH`) | moved to new `EntityTypes` class | Never reference the statics: `BuiltInRegistries.ENTITY_TYPE.getValue(Identifier)` is identical in both |
| `GuiGraphicsExtractor.entity(...)` | JOML `Vector3f`/`Quaternionf` params | JOML `Vector3fc`/`Quaternionfc` params (different bytecode descriptor!) | Resolve the method by name + param count via reflection, cache it (rare-fish-finder) |
| Screen effects (fire/underwater overlay) | `ScreenEffectRenderer.renderScreenEffect` with `renderFire`/`renderWater` helpers | renamed `submit(...)`, helpers inlined/removed | No clean shared target; fire-overlay feature deferred because of this |
| Rain splash particles + rain sound | `WeatherEffectRenderer.tickRainParticles(...)` | moved to `ClientLevel.tickWeatherEffects()` | Mixin pair (`WeatherTickLegacyMixin`/`WeatherTickModernMixin`) + plugin |
| Gamerule ids (server commands) | camelCase (`doDaylightCycle`, `doWeatherCycle`) | snake_case (`advance_time`, `advance_weather`) | Ops knowledge, not code |

## Non-obvious but identical in both (verified by javap)

- `FogEnvironment.isApplicable`/`setupFog` and all `FogData` public fields
- `LivingEntity.hasEffect`/`getEffect`, `MobEffects.DARKNESS/BLINDNESS/NAUSEA`
- `GameRenderer.displayItemActivation` and `bobHurt`
- `Gui`/`Hud` `extractTextureOverlay(GuiGraphicsExtractor, Identifier, float)`
- `EntityHitboxDebugRenderer.showHitboxes` drawing arrows via `Gizmos.arrow(Vec3, Vec3, I)`
- `Level.addParticle(ParticleOptions, DDDDDD)` and `FireworkRocketEntity.tick`'s trail call
- Totem burst: spawned in `ClientPacketListener.handleEntityEvent` via
  `ParticleEngine.createTrackingEmitter(Entity, ParticleOptions, I)` (NOT in
  `LivingEntity.handleEntityEvent`, whose lone `addParticle` is PORTAL particles)
- Leaf drips during rain: `LeavesBlock.makeDrippingWaterParticles` (separate
  from both rain rendering and ground splashes)
- Vanilla already has options for: vignette (Video Settings) and darkness
  pulsing (Accessibility) — do not duplicate

## How to verify before shipping

Both versions' mojmap jars live in the loom cache:
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/<version>/`

Check any member with `javap -p -classpath <jar> <class>` against BOTH
versions before referencing it directly. For a full audit, extract every
`net/minecraft` Field/Method reference from the built jar (`javap -c`) and
resolve each against both jars including superclasses; a reference missing
in either version means reflection, a registry lookup, or a version-gated
mixin pair. The one crash that reached users (2.5.2's `Minecraft.screen`)
happened because only one class had been audited, not the whole jar.
