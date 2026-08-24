# 26.1 vs 26.2

One jar runs on both versions. The runtime uses mojmap names directly, so
any renamed member is a hard crash on the other version. Add new finds here.

## This has now shipped broken twice, so read this part

2.5.2 and 2.8.0 both compiled cleanly, both were tested by hand, and both
crashed on 26.2 in the wild. Neither author was careless; both simply built
against the version their `gradle.properties` happened to name, and nothing
checked the other one. The table below existed for both incidents and did not
prevent either, because a table only helps someone who already suspects there
is something to look up.

So the rule is not "remember to check". The rule is:

**CI builds against both 26.1.2 and 26.2 on every push, and `publish` cannot
run unless both succeed** (`.github/workflows/publish.yml`, the `verify` job).
That gate is the actual protection. If you ever find yourself disabling it to
get a release out, you are about to reproduce 2.8.0.

What the gate does NOT catch, and what still needs a human:

- **Mixin `@Inject` descriptors are resolved at RUNTIME, not compile time.** A
  mixin targeting a signature that only exists on one version compiles happily
  and dies at `Initializing game` on the other. That was the whole 2.8.0 crash.
  After touching any mixin, LAUNCH both versions (`./gradlew runClient`, once
  per `minecraft_version`) and confirm the game reaches the main menu. To run
  26.2 in dev you must also set `fabric_version` to a 26.2 build; the 26.1 one
  refuses to load.
- **A descriptor that FITS is not one that MEANS the same thing.** See the
  `extractWindow` row below: on 26.2 its two ints are the mouse, not the window
  origin, so "delete two parameters until mixin stops complaining" produces a
  tab that renders at the cursor. Check the call site with `javap -c`, not just
  the arity with `javap -p`.

Tag releases as `vX.Y.Z` and nothing else: mc-publish derives the Modrinth
version name from the tag, so `v2.8.1-release` published a version called
"2.8.1-release".

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
| `AdvancementsScreen` extract methods | `extractInside(G,xo,yo)`, `extractWindow(G,xo,yo,mouseX,mouseY)`, `extractTooltips(G,mouseX,mouseY,xo,yo)` | origin dropped from all three: `extractInside(G)`, `extractWindow(G,mouseX,mouseY)`, `extractTooltips(G,mouseX,mouseY)` | mixin pair + plugin; recompute the origin from the screen size |
| Toast manager | `Minecraft.getToastManager()` | moved to `Minecraft.gui.toastManager()`, the Minecraft accessor is gone | resolve by name (`client/Toasts.java`) |

Identical in both, but not where you'd expect:

- Totem burst: `ClientPacketListener.handleEntityEvent` via
  `ParticleEngine.createTrackingEmitter`. The lone `addParticle` in
  `LivingEntity.handleEntityEvent` is PORTAL particles.
- Leaf drips in rain: `LeavesBlock.makeDrippingWaterParticles`, separate
  from both the rain rendering and the ground splashes.
- F3+B arrows: `EntityHitboxDebugRenderer.showHitboxes` via `Gizmos.arrow`.
- Vanilla already has settings for vignette, darkness pulsing, FOV
  effects, clouds, menu blur and lightning flashes (Accessibility).
  Don't duplicate them.

The two rows above are 2.8.0's crash: the Fish Collection tab was written and
tested against 26.1.2 only and hard-crashed 26.2 at `Initializing game`. Note
the shape of the trap. On 26.2 the two ints on `extractWindow` are the MOUSE,
not the origin (vanilla forwards `extractRenderState`'s own mouseX/mouseY), so
dropping two parameters to make the descriptor fit compiles, loads, and then
draws the tab wherever the cursor is. A descriptor that merely FITS is not the
same as one that means the same thing; check the call site with `javap -c`, not
just the arity.

Verifying: both mojmap jars sit in
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/`.
Run `javap -p` against BOTH before referencing anything directly. Before a
release, audit every `net/minecraft` reference in the built jar the same
way. 2.5.2 crashed on 26.2 because only one class got audited.
