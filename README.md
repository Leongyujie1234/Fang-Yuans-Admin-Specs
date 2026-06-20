# Great Titty's Admin Specs

An **admin-granted combat "Spec"** system for Minecraft **1.21.1 NeoForge**, inspired by
Gu Zhen Ren's *Reverend Insanity* (Reverend Insanity / 蛊真人).

The first spec implemented is **Rank 7 Gu Yue Fang Yuan**, with four signature moves.

> The mod is server-driven: only an operator can grant a spec to a player via `/admin spec set`.
> Once a spec is granted, the player can use the four bound move keys (default `1`, `2`, `3`, `4`).

---

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x (tested with 21.1.172)

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

The default keybindings (rebindable via Options → Controls → Admin Spec):

| Key | Move                              | Type      |
| --- | --------------------------------- | --------- |
| `1` | Reverse Flow Protection Seal      | Toggle    |
| `2` | Giant Hand                        | Cast      |
| `3` | Emperor Yama                      | Toggle + Summon |
| `4` | Five Finger Fist Heart Sword      | Channel   |

---

## Gu Yue Fang Yuan — Move Reference

### 1. Reverse Flow Protection Seal  (toggle, key `1`)

- Toggling on gives the player a **blue translucent shell** (visible to the player themselves).
- While ON, the player **cannot be damaged**, and any attack is reversed:
  - **Melee attacks** — the attacker takes the same damage they tried to deal, plus knockback.
  - **Projectiles** — the projectile is reflected straight back at the shooter.
- Reversing an attack drains the **Reverse Flow River**:
  - Melee reversal costs **5%** capacity.
  - Projectile reversal costs **10%** capacity.
  - When the river hits **0%**, the seal auto-disables with a chat message.
- While toggled OFF, the river refills at **~2% per second** up to 100%.
- Capacity starts at **100%** when the spec is granted.

### 2. Giant Hand  (cast, key `2`)

- Summons a giant hand **18 blocks above the player's look target**.
- The hand falls **very slowly** (about 5 blocks/second) — visually heavy.
- On landing, deals **12 hearts of damage** in a **5-block radius** with knockback.
- Cooldown: **12 seconds**.

### 3. Emperor Yama  (toggle + summon, key `3`)

- Press once to **assume the form**: gain creative-style **flight** and a **black translucent shell**.
- Once the form is active:
  - **Press again** (without sneak) to **summon a Yama Child** at the player's position.
  - **Sneak + press** to **release the form** (disable flight).
- Yama Children are baby-zombie entities that:
  - Walk toward the nearest enemy at increased speed.
  - **Self-detonate** when within 2.5 blocks of the target.
  - Deal **2× TNT damage** (48 HP at center) in the **same blast radius** as TNT (4 blocks).
  - Cause the same block destruction as a vanilla TNT explosion.
- Limits:
  - Max **3** Yama Children alive per player at once.
  - Summon cooldown: **3 seconds**.

### 4. Five Finger Fist Heart Sword  (channel, key `4`)

- On activation:
  - The player is **self-stunned** (Slowness VI + Mining Fatigue VI + Weakness VI) for the duration.
  - The player **announces "First finger!"** in chat (visible to all players on the server).
  - After **1 second**, a sword-light beam fires from the player's eye position along their look direction.
  - The beam is **near-instantaneous** (8 blocks/tick, ~80-block range).
  - The beam deals **6 hearts (12 HP)** of damage to the first entity it hits.
- The sequence then repeats for **"Second finger!"** and **"Third finger!"** (3 fingers total).
- After the third finger, the player's stun is cleared and the move enters a **10-second cooldown**.
- The player can rotate between fingers, so each finger can independently target a different direction.

---

## How It's Built (For Developers)

### Project structure

```
src/main/java/com/adminspec/
├── AdminSpecMod.java                - @Mod entrypoint
├── capability/
│   ├── PlayerSpecCapability.java    - NeoForge data attachment registration
│   ├── PlayerSpecData.java          - Per-player spec state (INBTSerializable)
│   └── SpecEvents.java              - Server tick + damage absorption hooks
├── command/
│   └── AdminSpecCommand.java        - /admin spec set|clear|list
├── entity/
│   ├── ModEntities.java             - Entity registration
│   ├── GiantHandEntity.java
│   ├── YamaChildEntity.java
│   └── SwordLightEntity.java
├── moves/
│   ├── ModMoves.java
│   └── guyue/
│       ├── ReverseFlowProtectionSealMove.java
│       ├── GiantHandMove.java
│       ├── EmperorYamaMove.java
│       └── FiveFingerFistHeartSwordMove.java
├── network/
│   ├── ModPayloads.java             - payload registration
│   ├── ActivateMovePayload.java     - client -> server: "I pressed move N"
│   └── SpecStatePayload.java        - server -> client: spec state snapshot
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
    ├── ClientSpecState.java         - client-side spec state cache
    ├── OutlineRenderer.java         - blue/black glow shell rendering
    ├── GiantHandRenderer.java
    └── SwordLightRenderer.java
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

---

## Build from Source

```bash
chmod +x ./gradlew
./gradlew build
# jar will be at build/libs/adminspec-<version>.jar
```

Requires JDK 21.

---

## Credits

- Spec concept and character: **Gu Zhen Ren** — *Reverend Insanity* (Reverend Insanity / 蛊真人).
- Mod code: MIT-licensed, see `META-INF/neoforge.mods.toml` for details.

---

## Disclaimer

This is a fan-made gameplay mod. The character and move names belong to the original author
of *Reverend Insanity*. No commercial use is intended.
