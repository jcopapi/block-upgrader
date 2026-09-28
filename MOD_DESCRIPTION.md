# Block Upgrader

Upgrade, construct, and modify placed blocks with materials, tool actions, and timed work. Block Upgrader adds a data-driven recipe system that happens in the world rather than inside a crafting grid.

Recipes can offer several paths from the same block, keep compatible inventories and block data, and define how an upgrade is reversed. Minecraft 1.21.1 and NeoForge are the only required mods. KubeJS, JEI, Create, Sophisticated Storage, and Iron Furnaces are optional integrations.

## What you can make

- **Block upgrades:** turn a placed block into another block, instantly or after construction time.
- **Unfinished blocks:** make a newly placed block require materials or actions before it becomes usable.
- **Item-producing operations:** process a placed block into item outputs, optionally removing the source.
- **Multiple paths:** define several recipes with the same source block and let players choose one.
- **Reversible upgrades:** configure a tool and number of actions to restore the previous block and refund supplied materials.
- **Pack-specific interactions:** accept an item or an item tag, give it a readable label, choose left/right/both mouse inputs, and configure sounds, particles, impact, and cooldowns.
- **Optional compatibility:** preserve suitable inventories, properties, and block-entity data. Compatible double chests and two-block structures upgrade together; double chests charge materials for both halves.

Hold **Shift** while looking at a supported block to see its options. Scroll while holding Shift to choose a path, then Shift-right-click to select it. Right-click with a required material to deposit it. The HUD shows what remains. An unfinished selection can be canceled for a material refund; completed upgrades can be reversed with their configured tool when the target inventory is empty.

## Example: a timed upgrade

This example shows the difference between a **required stage** and an **accelerator**. The hammer actions are part of the recipe and must be completed. The pickaxe actions shorten the timer; the timer can still finish naturally. The `#c:hammers` tag is illustrative and must be supplied by your pack.

```json
{
  "source": "minecraft:furnace",
  "result": "minecraft:blast_furnace",
  "title": "Upgrade to Blast Furnace",
  "description": "Build a faster furnace for ores.",
  "materials": [
    {"item": "minecraft:iron_ingot", "count": 5, "label": "Iron Ingot"},
    {"item": "minecraft:smooth_stone", "count": 3}
  ],
  "stages": [
    {"type": "TOOL_ACTION", "item": "#c:hammers", "actions": 3,
     "input": "RIGHT", "damage": 1, "label": "Hammer"}
  ],
  "build_time": 1200,
  "accelerators": [
    {"kind": "tool", "item": "#minecraft:pickaxes", "actions": 3,
     "reduction": 600, "input": "RIGHT", "damage": 1, "label": "Pickaxe"}
  ],
  "transfer": "copy_inventory",
  "downgrade": {"tool": "#minecraft:pickaxes", "hits": 3, "label": "Pickaxe"}
}
```

`build_time` and `reduction` are in game ticks: 1,200 ticks is 60 seconds, and the three pickaxe actions remove 600 ticks (30 seconds) in total. Material counts are shown on JEI item slots. The custom `label` changes the HUD and action description without restricting the tag to one specific item. A separate JSON file with the same `source` and a different `result` creates another selectable path.

Put the recipe at `data/<namespace>/jco_block_upgrades/<name>.json`. For example, `data/example/jco_block_upgrades/furnace_to_blast_furnace.json` has the recipe ID `example:furnace_to_blast_furnace`. This directory name is the loader's current internal path.

## Example: construction after placement

An incomplete recipe uses the **same** source and result block. The ordinary Smithing Table item is placed first; that instance then needs the configured work before it becomes usable. Existing tables are unaffected.

```json
{
  "source": "minecraft:smithing_table",
  "result": "minecraft:smithing_table",
  "placed_incomplete": true,
  "title": "Finish Smithing Table",
  "stages": [
    {"type": "ITEM_APPLICATION", "item": "minecraft:iron_ingot", "actions": 2,
     "input": "RIGHT", "consume": 1, "label": "Rivet"},
    {"type": "TOOL_ACTION", "item": "#minecraft:pickaxes", "actions": 3,
     "input": "RIGHT", "damage": 1, "label": "Pickaxe"}
  ]
}
```

The two stages run in order: apply two ingots, then strike three times with any pickaxe. `consume` spends items, while `damage` spends tool durability. Add `materials` or `build_time` if construction should require them too. Incomplete placement recipes stay in the in-world HUD and are intentionally omitted from JEI. JEI's **Upgrade** category shows recipes with materials instead.

## Example: items instead of another block

Use `outputs` without `result` to make a placed block produce items. `remove_block_on_complete` controls whether the source is consumed.

```json
{
  "source": "minecraft:cauldron",
  "title": "Salvage Materials",
  "materials": [{"item": "minecraft:iron_ingot", "count": 1}],
  "outputs": [{"item": "minecraft:iron_nugget", "count": 4}],
  "remove_block_on_complete": false
}
```

These examples show separate recipe shapes. Choose a result block **or** item outputs for one definition.

## KubeJS and optional mods

KubeJS can declare the same routes and add Java-backed transfer or completion callbacks when a datapack field is not enough. This example loads meaningfully only with Sophisticated Storage installed; its chest inventory, upgrades, and wood type are preserved by the integration.

```js
BlockUpgrades.create('example:chest_to_copper', upgrade => {
  upgrade.block('sophisticatedstorage:chest')
    .result('sophisticatedstorage:copper_chest')
    .title('Upgrade to Copper Chest')
    .material('minecraft:copper_ingot', 8,
      feedback => feedback.label('Copper Ingot'))
    .transferMode('sophisticated_storage')
    .downgrade('#minecraft:axes', 3, 'Axe')
})
```

Put scripts at the top level of `kubejs/server_scripts/`. A script can override a datapack recipe by ID or remove one with `BlockUpgrades.disable('namespace:recipe_name')`. Only include the chest script when Sophisticated Storage is installed. For optional datapack routes, use `"required_mods": ["sophisticatedstorage"]`; absent mods cause those routes to be skipped, not required for Block Upgrader to load.

For behavior that cannot be expressed as data, KubeJS can also call `.transfer(context => context.copyInventory())` on a block conversion, or `.onComplete(context => { context.drop(Item.of('minecraft:iron_nugget', 4)); context.removeBlock() })` on an item-producing operation. These are alternatives to the built-in transfer modes and static `outputs`/`remove_block_on_complete` fields.

## Other recipe controls

Datapacks can also set `enabled`, `display_item`, `hud_range`, `preserve_properties`, `manual_completes`, `result_data`, and `completion_feedback`. For example, these fields can be added to a compatible block-to-block recipe:

```json
{
  "enabled": true,
  "required_mods": ["sophisticatedstorage"],
  "display_item": "sophisticatedstorage:copper_chest",
  "hud_range": 8,
  "preserve_properties": true,
  "result_data": {"woodType": "spruce"},
  "completion_feedback": {
    "sound": {"id": "minecraft:block.anvil.use", "volume": 0.8, "pitch": 1.1},
    "particles": {"id": "minecraft:happy_villager", "count": 12, "spread": 0.3, "speed": 0.04},
    "impact": {"amplitude": 0.06, "ticks": 8}
  }
}
```

This is a field fragment, not a complete recipe. `result_data` merges generic data into the new block entity after transfer; here it deliberately sets a chest's wood type. Without it, Sophisticated Storage keeps the original wood. The built-in transfer modes are `none`, `copy_data`, `copy_inventory`, and `sophisticated_storage`. `manual_completes` lets configured work finish a timed recipe early.

Materials, stages, and accelerators can customize `label`, `input`, `display_item`, `cooldown`, `sound`, `particles`, and `impact`. Feedback may also define a nested `completed` effect for the last action in a row. Stages support `TOOL_ACTION` and `ITEM_APPLICATION` with configurable `consume` and `damage`. Accelerators support any matching item or tag, configurable actions and time reduction, per-action or grouped reduction, consumption or durability cost, and repeatability. KubeJS additionally exposes custom transfer and completion callbacks, including item drops and source removal.

With JEI installed, the **Upgrade** category shows source and result blocks, material counts, construction time, and required work or accelerators. Create's clipboard can record a selected upgrade's material list. The client config `config/block_upgrader-client.toml` controls HUD scale.

More examples and configuration notes are in the [README](README.md). Source code is available under the MIT license.
