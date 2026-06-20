# Fang Yuan's Admin Specs

An **admin-granted combat "Spec"** system for Minecraft **1.21.1 NeoForge**, inspired by
Gu Zhen Ren's *Reverend Insanity* (Reverend Insanity / 蛊真人).

The first spec implemented is **Rank 7 Liu Guan Yi** (Gu Yue Fang Yuan), with four
signature moves.

> The mod is server-driven: only an operator can grant a spec to a player via
> `/admin spec set`. Once a spec is granted, the player can use the four bound move
> keys (default `1`, `2`, `3`, `4`).

---

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x (tested with 21.1.172)
- JDK 21

---

## Installation

1. Install NeoForge 21.1.x for Minecraft 1.21.1.
2. Drop `adminspec-1.0.0.jar` into your `.minecraft/mods` folder (server *and* client need it).
3. Launch the game.

---

## Commands

All commands require **operator permission level 2**.

| Command                                   | Effect                                            |
| ----------------------------------------- | ------------------------------------------------- |
| `/admin spec set <specname>`              | Grant spec `specname` to yourself.                |
| `/admin spec set <specname> <player>`     | Grant spec `specname` to `<player>`.              |
| `/admin spec clear`                       | Remove your currently active spec.                |
| `/admin spec list`                        | List all registered spec ids.                     |

**First spec id:** `liu_guan_yi`

```
/admin spec set liu_guan_yi
```

---

## Controls

The default keybindings (rebindable via Options → Controls → Admin Spec). Keys are bound
in spec move order, so for Liu Guan Yi:

| Key | Move                              | Type             |
| --- | --------------------------------- | ---------------- |
| `1` | Sword Escape                      | Dash             |
| `2` | Reverse Flow Protection Seal      | Toggle           |
| `3` | Ancient Sword Dragon Transformation | Toggle + Flight |
| `4` | Yama Children                     | Summon           |

---

## Liu Guan Yi — Move Reference

### 1. Sword Escape  (dash, key `1`)

- The player dashes forward as a streak of sword light along their look direction
  (up to ~12 blocks, stopping at walls).
- Movement is **smooth and velocity-based**: a burst that decays exponentially
  (`speed *= 0.80`) over a 10-tick duration. No jitter, no per-tick teleport.
- Deals **2 HP** of damage to every entity the dash passes through; each entity is
  hit **at most once per dash**.
- The **first** entity that dies to the dash is **beheaded**: the appropriate head
  block flies off as a `flying_head` entity (player/zombie/skeleton/creeper/wither
  skeleton/piglin/dragon heads are all supported).
- While dashing the caster is invisible and invulnerable. Both effects clear when the
  dash ends.
- Cooldown: **5 seconds** (100 ticks).

### 2. Reverse Flow Protection Seal  (toggle, key `2`)

- Toggling on surrounds you with a **flowing blue water robe** (rising + descending
  helices, splashes and mist on every player who has the seal active).
- While ON, you **cannot be damaged**, and the attack is reversed:
  - **Melee attacks** — the attacker takes the same damage they tried to deal.
  - **Projectiles** — the projectile is reflected straight back at the shooter.
- Reversing an attack drains the **Reverse Flow River**:
  - Melee reversal costs `max(2%, damage × 0.5%)`.
  - Projectile reversal costs **3%** capacity.
  - When the river hits **0%**, the seal auto-disables with a chat message.
- While toggled OFF, the river refills at **0.1% per tick (~2% per second)** up to 100%.
- Capacity starts at **100%** when the spec is granted.

### 3. Ancient Sword Dragon Transformation  (toggle + flight, key `3`)

- Press once to **transform**: the screen flashes white, an explosion burst plays,
  and the player is launched upward. The dragon model then **mutates in phases**:
  - ticks 0–5:   nothing yet (launch animation)
  - ticks 5–20:  head + neck emerge
  - ticks 20–40: front + middle body grow
  - ticks 40–60: rear body, tail and tail-fin complete the dragon
  - ticks 60+:   full dragon — the player model is replaced entirely
- Once transformed the player has a **custom velocity-based flight** model:
  - **WASD** = fly in look direction / strafe
  - **Space** = ascend, **Shift** = descend
  - Capped top speed, with friction for a smooth "barrel-roll" feel.
  - The dragon form also grants **+20 armor** and **+8 armor toughness**.
- **Left-click (M1) = Sword Qi Breath**: a sword-light beam of CRIT/ENCHANTED_HIT
  particles extending 16 blocks along your look vector, dealing **6 HP** to the first
  entity in its path (within 2 blocks of the beam axis). 3-second cooldown.
- While transformed, **all melee swings and block/entity interactions are cancelled**
  so the only attack is the breath.
- Duration: **5 minutes** (6000 ticks), after which it auto-deselects and restores
  your inventory.
- **Press `3` again** to detransform early (restores inventory, flight and attributes).

### 4. Yama Children  (summon, key `4`)

- Spawns a **flying baby zombie** ("Yama Child") 2 blocks in front of the caster.
- Yama Children are **flying**: gravity is disabled and they home in on the nearest
  living target at ~3.6 blocks/second, intercepting in full 3D.
- When within 2.5 blocks of their target they **light a 1-second fuse** (visible
  flame + smoke), then **self-detonate**.
- The blast deals **2× TNT damage (48 HP at centre)** with distance falloff over a
  **4-block radius**, plus TNT-equivalent knockback. It also triggers a real TNT
  explosion for block destruction.
- **Block recovery:** every block destroyed by the blast is snapshotted (state +
  block-entity NBT) and **automatically restored after 2 minutes** (2400 ticks).
- Limits:
  - Max **3** Yama Children alive per player at once.
  - Summon cooldown: **3 seconds**.
  - Each child self-despawns after 30 seconds if it hasn't detonated.

---

## How It's Built (For Developers)

### Project structure

```
src/main/java/com/adminspec/
├── AdminSpecMod.java                - @Mod entrypoint
├── ModSounds.java                   - custom sound events (sword_escape)
├── capability/
│   ├── PlayerSpecCapability.java    - NeoForge data attachment registration
│   ├── PlayerSpecData.java          - Per-player spec state (INBTSerializable)
│   ├── SpecEvents.java              - Server tick, damage/reflection, dragon interaction locks
│   └── BlockRecoveryManager.java    - Snapshots & restores blocks destroyed by Yama blasts
├── command/
│   └── AdminSpecCommand.java        - /admin spec set|clear|list
├── entity/
│   ├── ModEntities.java             - Entity registration (yama_child, flying_head)
│   ├── YamaChildEntity.java         - Flying, fusing, exploding baby zombie
│   └── FlyingHeadEntity.java        - Decapitated head projectile from Sword Escape
├── moves/
│   ├── ModMoves.java
│   └── guyue/
│       ├── SwordEscapeMove.java
│       ├── ReverseFlowProtectionSealMove.java
│       ├── AncientSwordDragonTransformationMove.java
│       └── YamaChildrenMove.java
├── network/
│   ├── ModPayloads.java             - payload registration
│   ├── ActivateMovePayload.java     - client -> server: "I pressed move N"
│   ├── SpecStatePayload.java        - server -> client: spec state snapshot
│   ├── DragonFormPayload.java       - server -> client: dragon form toggle
│   ├── DragonFlightInputPayload.java- client -> server: flight WASD/jump/sneak
│   ├── DragonBreathPayload.java
│   ├── DragonBreathVfxPayload.java  - 7-field payload w/ custom StreamCodec
│   └── SwordEscapeBeamPayload.java  - server -> client: sword-light beam VFX
├── spec/
│   ├── Spec.java
│   ├── SpecMove.java
│   ├── SpecRegistry.java
│   ├── MoveContext.java
│   └── guyue/
│       └── LiuGuanYiSpec.java       - registers the 4 moves into a Spec
└── client/
    ├── ClientSetup.java             - keybinds + renderer registration
    ├── ClientKeyHandler.java        - keybind -> ActivateMovePayload
    ├── ClientSpecState.java         - client-side spec state cache + transform VFX
    ├── MoveKeybinds.java            - keybind registry
    ├── ReverseFlowParticleHandler.java - water-robe particle FX (client)
    ├── ReverseFlowHud.java          - river capacity HUD bar
    ├── ClientDragonFormRenderer.java   - Bedrock geo dragon renderer (phased + full)
    ├── ClientDragonFormState.java   - toggles third-person camera in dragon form
    ├── DragonBreathHandler.java     - client-side breath VFX
    ├── ClientBeamManager.java / ClientBeamSpawner.java / BeamRenderHandler.java - sword-light beams
    ├── FlyingHeadRenderer.java      - flying head block renderer
    └── YamaChildRenderer.java       - black Yama Child texture + fuse sparks
```

### Assets

```
src/main/resources/assets/adminspec/
├── lang/en_us.json
├── sounds.json + sounds/sword_escape.ogg
├── models/entity/ancient_sword_dragon.geo.json   - Bedrock geo model (9 bones)
└── textures/entity/
    ├── ancient_sword_dragon.png
    └── yama_child.png
```

### Adding a new spec

1. Create a package `com.adminspec.spec.<your_spec>` with a class like `MySpecSpec.register()`:
   ```java
   public static void register() {
       Spec spec = new Spec(
           "my_spec_id",
           Component.literal("My Spec"),
           Component.literal("Description..."),
           List.of(new MyMove1(), new MyMove2(), ...)
       );
       SpecRegistry.register(spec);
   }
   ```
2. Add `MySpecSpec.register();` to `SpecRegistry.registerDefaults()`.
3. Create your moves under `com.adminspec.moves.<your_spec>` extending `SpecMove`.
4. Grant it in-game: `/admin spec set my_spec_id`.

The first four keybinds (`1`,`2`,`3`,`4`) auto-bind to the moves in declaration order;
any extra moves default to unbound and can be assigned in Controls.

---

## Build from Source

```bash
./gradlew build
# jar will be at build/libs/adminspec-<version>.jar
```

---

## Credits

- Spec concept and character: **Gu Zhen Ren** — *Reverend Insanity* (Reverend Insanity / 蛊真人).
- Mod code: MIT-licensed, see `META-INF/neoforge.mods.toml` for details.

---

## Disclaimer

This is a fan-made gameplay mod. The character and move names belong to the original author
of *Reverend Insanity*. No commercial use is intended.
