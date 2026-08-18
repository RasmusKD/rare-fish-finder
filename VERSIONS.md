# 26.1 vs 26.2

One jar runs on both versions. The runtime uses mojmap names directly, so
any renamed member is a hard crash on the other version. Add new finds here.

| What | 26.1.x | 26.2 | Fix |
|---|---|---|---|
| HUD class | `client.gui.Gui` | `client.gui.Hud`, members identical | mixin pair + `ClearSightMixinPlugin` |
| Screen on `Minecraft` | field `screen` | method `screen()` | reflection by name (rare-fish-finder) |
| `Minecraft.setScreen` | exists | gone | use `setScreenAndShow`, exists in both |
| Entity type constants | on `EntityType` | moved to `EntityTypes` | `BuiltInRegistries.ENTITY_TYPE.getValue(id)` instead |
| `GuiGraphicsExtractor.entity` | `Vector3f`/`Quaternionf` | `Vector3fc`/`Quaternionfc` (other descriptor) | resolve by name + param count (rare-fish-finder) |
| Screen effect helpers | `renderScreenEffect`, `renderFire`, `renderWater`, `renderTex` | `submit`, `submitFire`, `submitWater`, `submitBlockSprite` | mixin pair + plugin |
| Rain splash + sound | `WeatherEffectRenderer.tickRainParticles` | `ClientLevel.tickWeatherEffects` | mixin pair + plugin |
| Gamerule ids | `doDaylightCycle`, `doWeatherCycle` | `advance_time`, `advance_weather` | server commands only |

Identical in both, but not where you'd expect:

- Totem burst: `ClientPacketListener.handleEntityEvent` via
  `ParticleEngine.createTrackingEmitter`. The lone `addParticle` in
  `LivingEntity.handleEntityEvent` is PORTAL particles.
- Leaf drips in rain: `LeavesBlock.makeDrippingWaterParticles`, separate
  from both the rain rendering and the ground splashes.
- F3+B arrows: `EntityHitboxDebugRenderer.showHitboxes` via `Gizmos.arrow`.
- Vanilla already has settings for vignette, darkness pulsing and FOV
  effects. Don't duplicate them.

Verifying: both mojmap jars sit in
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/`.
Run `javap -p` against BOTH before referencing anything directly. Before a
release, audit every `net/minecraft` reference in the built jar the same
way. 2.5.2 crashed on 26.2 because only one class got audited.
