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

### Minecraft and Fabric API move TOGETHER

Overriding `minecraft_version` alone is not enough, and the way it fails is
designed to mislead. Building 26.2 while `fabric_version` still names a 26.1
build dies during Loom's Minecraft SETUP, before a line of this mod compiles:

```
Failed to setup Minecraft: Failed to apply transformation to
net/minecraft/data/tags/TagAppender.class
Caused by: Interface .../FabricTagAppender attempted to use a type variable
named E which is not present in the TagAppender class
```

That reads like a Loom bug and is not one. It is Fabric API's INTERFACE
INJECTION: `FabricTagAppender` injects into Minecraft's `TagAppender`, whose
generics changed in 26.2, so a 26.1 Fabric API cannot be injected into a 26.2
Minecraft. Pinning or downgrading Loom does nothing - all of `1.17-SNAPSHOT`,
`1.17.19` and `1.17.17` fail identically. Pair the two versions and it builds
cold on both:

| Minecraft | Fabric API |
|---|---|
| 26.1.2 | 0.155.2+26.1.2 |
| 26.2 | 0.158.0+26.2 |

The CI matrix sets both per leg for exactly this reason.

Note the trap for local work: once Loom has cached a successfully-prepared
Minecraft jar, the failing injection step never runs again, so a warm machine
builds a mismatched pair happily while CI fails every time. `--refresh-
dependencies` reproduces CI's view. That gap is why this looked like an
unfixable upstream bug for an afternoon.

Do NOT make the 26.2 leg `continue-on-error` to get the board green. That
converts the one check that would have caught 2.5.2 and 2.8.0 into decoration.

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

## Compat mixins against other mods

A `@Unique static final` field declared in a mixin is merged into the TARGET
class and initialised in ITS `<clinit>`. That is fine for a vanilla screen the
game loads at the right moment, and a trap for a third-party one: Better
Advancements class-loads its screen from its config handler during
`onInitializeClient`, so an `ItemStack` held in our mixin was constructed
before item components were bound and killed the game at startup with
"Components not bound yet" - for every user of that mod, and only for them.

Hold such values in an ordinary class of ours and reach through a static
accessor (`FishTabView.icon()`). That class is not loaded until something
renders, which is well after binding. The vanilla tab mixins never hit this
only because their state already lived in FishTabView.

Gate every mod-specific mixin on the mod actually being present
(`FabricLoader.isModLoaded`, see RareFishFinderMixinPlugin). Mixin aborts the
whole config when a target class is missing, so one ungated compat mixin takes
every other mixin in this mod down with it.
