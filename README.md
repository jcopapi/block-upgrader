# Block Upgrader

A data-driven, reversible in-world block upgrade system for Minecraft 1.21.1 / NeoForge 21.1.x. Minecraft and NeoForge are the only required mods. KubeJS, Create, Sophisticated Storage, Iron Furnaces and Just Enough Items (JEI) are detected when installed.

## Playing

Look at a supported block while holding Shift to open its recipe. Scroll while holding Shift to choose a route. Shift-right-click to select; right-click while holding each material to contribute one at a time. Once all materials are supplied, the block changes to its target. A timed recipe finishes automatically; three right-clicks with any pickaxe can accelerate the bundled furnace recipes. Sophisticated Storage tier changes are immediate.

Shift-right-click an unfinished upgrade with an empty hand to cancel and return contributed materials. Once complete, hold Shift or a matching downgrade tool to see the reversal panel. Right-click three times with the configured tool to restore the previous block and receive its deposited materials. Full inventories prevent reversal. Upgrade history follows the block when broken and replaced, including its previous block state. The HUD displays generic tool names for item tags: for example, any pickaxe appears as **Pickaxe**.

The client config `config/block_upgrader-client.toml` has `hudScale` (0.5–2.0, default 1.0). This scales the recipe panels without changing server recipes. `build_time: 0` removes a recipe's timer.

## Just Enough Items

With JEI installed, open the **Block Upgrader** recipe category from a source block's uses, a result block's recipes, or a required material. The compact, vanilla-style view shows block → upgraded block, material icons and counts, then tool actions with mouse-button icons. A clock marks timed recipes. Hover the arrow for the recipe name and description, or a `+N` marker for additional materials or actions. Item tags cycle through valid alternatives, so any matching Pickaxe or Axe can be found in JEI.

JEI receives the active recipe catalog from the server. Datapack and KubeJS overrides therefore appear correctly in multiplayer and refresh after `/reload` or a successful script reload. JEI is optional; the block-upgrade HUD and gameplay work without it.

## Built-in examples

The JAR includes 24 datapack recipes. The base game provides Furnace → Blast Furnace and Furnace → Smoker. Both require multiple materials, have short descriptions, build for 1,200 ticks, and can be accelerated by three right-clicks with any pickaxe. A newly placed Smithing Table starts unfinished: right-click it three times with any pickaxe to make it usable. Existing tables remain usable. Breaking an unfinished table returns the ordinary table item; placing it again starts a fresh construction. Shift-right-clicking with an empty hand picks up an unfinished table and returns contributed materials.

With Sophisticated Storage installed, chests and barrels progress through base → copper → iron → gold → diamond → netherite. They require two materials per tier, convert instantly, and preserve inventory, upgrades, and source wood. The preview copies the source wood and tint to the result icon.

With Iron Furnaces installed, a vanilla furnace can become copper or iron. The chain then includes copper → iron → gold → diamond → emerald or crystal → obsidian → netherite → million. Every tier requires two materials and uses the timed pickaxe example. Vanilla-to-Iron transfers inventory slots; Iron-to-Iron transfers compatible block-entity data. These routes load only when Iron Furnaces is present.

The included Iron Furnace Reburn 0.4.5 release currently crashes its own dedicated-server startup due to a client renderer class load. The optional route files are still packaged for client play. The independent vanilla and Sophisticated Storage GameTests run on a dedicated server.

## Datapacks

Place a definition in `data/<namespace>/jco_block_upgrades/<name>.json`. The ID is `<namespace>:<name>`. A higher-priority datapack can replace a bundled recipe at the same path or disable it with `{"enabled":false}`. `/reload` refreshes definitions. Invalid recipes are logged and skipped individually.

```json
{
  "source": "minecraft:furnace",
  "result": "minecraft:blast_furnace",
  "title": "Upgrade to Blast Furnace",
  "description": "Smelts ores faster than a furnace.",
  "materials": [
    {"item": "minecraft:iron_ingot", "count": 5},
    {"item": "minecraft:smooth_stone", "count": 3},
    {"item": "minecraft:coal", "count": 2}
  ],
  "build_time": 1200,
  "accelerators": [
    {"kind": "tool", "item": "#minecraft:pickaxes", "actions": 3,
     "reduction": 1200, "input": "RIGHT", "display_item": "minecraft:iron_pickaxe"}
  ],
  "transfer": "copy_data",
  "downgrade": {"tool": "#minecraft:pickaxes", "hits": 3}
}
```

Fields: `enabled`, `required_mods`, `source`, `result` or `outputs`, `placed_incomplete`, `title`, `description`, `display_item`, `hud_range`, `materials`, `stages`, `build_time`, `manual_completes`, `accelerators`, `preserve_properties`, `transfer`, `result_data`, `remove_block_on_complete`, `completion_feedback`, and `downgrade`. Select items by registry ID or item tag (`#minecraft:pickaxes`). Stage types are `TOOL_ACTION` and `ITEM_APPLICATION`. Stage and accelerator inputs can be `LEFT`, `RIGHT`, or `BOTH`. Feedback supports sound, particles, impact, icon, cooldown and completion effects. The included JSON files provide working combinations.

`transfer` supports `none`, `copy_data`, `copy_inventory`, and `sophisticated_storage`. With `none`, nonempty source inventories block conversion. `copy_data` is for block entities that understand the same saved data, `copy_inventory` maps source slots to target slots without copying processing timers, and `sophisticated_storage` handles capacity and contents of compatible Sophisticated tiers. Source inventories are restored on failed conversion. `result_data` is a JSON object merged into the new block entity after transfer. It cannot replace `id`, `x`, `y`, or `z`.

For example, to set Sophisticated Storage wood through the same generic system used for any block entity:

```json
"result_data": {"woodType": "spruce"}
```

Without `result_data`, the original wood is preserved. The optional integration refuses linked double chests and controller-linked storage until separated.

## KubeJS

Declare recipes at top level in `kubejs/server_scripts/`. Scripts take priority over datapacks and can disable a bundled route with `BlockUpgrades.disable('jco_upgrades:furnace_to_smoker')`. KubeJS is optional.

```js
BlockUpgrades.create('pack:oak_copper_chest', upgrade => {
  upgrade.block('sophisticatedstorage:chest')
    .result('sophisticatedstorage:copper_chest')
    .title('Upgrade to Oak Copper Chest')
    .material('minecraft:copper_ingot', 8)
    .material('minecraft:oak_planks', 2)
    .transferMode('sophisticated_storage')
    .resultData("{woodType:'oak'}")
    .downgrade('#minecraft:axes', 3)
})
```

To make an ordinary block require construction when placed, set `placed_incomplete` to `true` with the same `source` and `result`. The bundled Smithing Table recipe is a working example:

```json
{
  "source": "minecraft:smithing_table",
  "result": "minecraft:smithing_table",
  "placed_incomplete": true,
  "title": "Finish Smithing Table",
  "stages": [{"type": "TOOL_ACTION", "item": "#minecraft:pickaxes",
              "actions": 3, "input": "RIGHT"}]
}
```

This uses the normal Smithing Table item. Placement marks that instance unfinished; the table cannot be used until its actions and any configured materials are complete. Any pre-existing table is unaffected. Recipes may add `materials` and `build_time`; materials deposited in an unfinished block are dropped if it is broken. To apply this behavior to another block, change both endpoint IDs. KubeJS can set the same rule with `.block('minecraft:smithing_table').result('minecraft:smithing_table').placedIncomplete(true)` and can override or disable the bundled recipe by ID. Placement recipes cannot transfer block-entity data or produce a different block; use an ordinary upgrade recipe for transformations.

With Create installed, hold its clipboard and right-click a supported block to append the selected material list; the clipboard hint appears in the HUD. `/block_upgrader status` and `/block_upgrader cancel` are administrator commands.

## Build and license

Build with Java 21 and Gradle using `./gradlew build` (`.\gradlew.bat build` on Windows). The JAR is `build/libs/block-upgrader-<version>.jar`.

The source references optional integration APIs at compile time but does not bundle or require them at runtime. To compile, put the NeoForge 1.21.1 JARs for KubeJS (`2101.7.2-build.377`), Rhino (`2101.2.8-build.91`), Create (`6.0.10`), Sophisticated Storage (`1.5.91.2127`), Sophisticated Core (`1.5.1.2341`), and JEI (`19.57.0.447`) in `local-mods/`, or point `BLOCK_UPGRADER_MODS_DIR` at a directory containing them. Gradle ignores that directory for Git.

Source code and the distributed JAR are licensed under MIT; see `LICENSE`.
