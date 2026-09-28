# Block Upgrader

A data-driven, reversible in-world block upgrade system for Minecraft 1.21.1 / NeoForge 21.1.x. Minecraft and NeoForge are the only required mods. KubeJS, Create, Sophisticated Storage, Iron Furnaces and Just Enough Items (JEI) are detected when installed.

See [CHANGELOG.md](CHANGELOG.md) for release notes.

## Playing

Look at a supported block while holding Shift to open its recipe. Scroll while holding Shift to choose a route, then Shift-right-click to select it. Right-click while holding each material to contribute one at a time. Other items still interact with the selected block normally. Once all materials are supplied, the block changes to its target. A timed recipe finishes automatically; three right-clicks with any pickaxe can accelerate the bundled furnace recipes. Sophisticated Storage tier changes are immediate.

Shift-right-click an unfinished upgrade with an empty hand to cancel and return contributed materials. Once complete, hold Shift or a matching downgrade tool to see the reversal panel. Right-click three times with the configured tool to restore the previous block and receive its deposited materials. Full inventories prevent reversal. Upgrade history follows the block when broken and replaced, including its previous block state. The HUD displays generic tool names for item tags: for example, any pickaxe appears as **Pickaxe**.

The client config `config/block_upgrader-client.toml` has `hudScale` (0.5–2.0, default 1.0). This scales the recipe panels without changing server recipes. `build_time: 0` removes a recipe's timer.

## Just Enough Items

With JEI installed, open the **Upgrade** recipe category (iron pickaxe icon) from a source block's uses, a result block's recipes, a required material, or the iron pickaxe catalyst. The recipe card shows the source and result blocks, title and description, material slots, optional construction time, and required work or accelerators. Hover an accelerator for its use count and time reduction, the description for its full text, or a `+N` marker for extra entries. Material quantities appear on the item icons, and item tags cycle through valid alternatives. Construction recipes marked `placed_incomplete` and recipes without materials are intentionally omitted from JEI; they remain available in the in-world HUD. JEI controls category tab order in `config/jei/client/recipe-category-sort-order.ini`; put `\=jco_upgrades:block_upgrade` immediately after `\=minecraft:crafting` to place Upgrade second.

JEI receives the active recipe catalog from the server. Datapack and KubeJS overrides therefore appear correctly in multiplayer and refresh after `/reload` or a successful script reload. JEI is optional; the block-upgrade HUD and gameplay work without it.

## Built-in examples

The JAR includes 24 datapack recipes. The base game provides Furnace → Blast Furnace and Furnace → Smoker. Both require multiple materials, have short descriptions, build for 1,200 ticks, and can be accelerated by three right-clicks with any pickaxe. A newly placed Smithing Table starts unfinished: right-click it three times with any pickaxe to make it usable. Existing tables remain usable. Breaking an unfinished table returns the ordinary table item; placing it again starts a fresh construction. Shift-right-clicking with an empty hand picks up an unfinished table and returns contributed materials.

With Sophisticated Storage installed, chests and barrels progress through base → copper → iron → gold → diamond → netherite. They require only the tier's metal or gem, convert instantly, and preserve inventory, upgrades, and source wood. The preview copies the source wood and tint to the result icon.

A double chest is treated as one upgrade. Either half accepts materials, the recipe charges twice the normal amount, and both halves change together while keeping each half's contents and wood. The HUD shows the combined material total. An intact upgraded pair is undone together after both inventories are emptied; each half also keeps its own refund history if the pair is later separated. This automatic pairing also applies to compatible data-driven or KubeJS recipes whose source and result both have Minecraft-style chest `type`/`facing` or two-block `half`/bed `part` states, including doors, tall blocks, and beds. Other multiblock layouts require a dedicated integration because their inventories and structure rules vary.

With Iron Furnaces installed, a vanilla furnace can become copper or iron. The chain then includes copper → iron → gold → diamond → emerald or crystal → obsidian → netherite → million. Every tier requires two materials and uses the timed pickaxe example. Vanilla-to-Iron transfers inventory slots; Iron-to-Iron transfers compatible block-entity data. These routes load only when Iron Furnaces is present.

The Iron Furnaces routes remain optional and are skipped when that mod is absent. The independent vanilla and Sophisticated Storage GameTests run on a dedicated server.

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
  "downgrade": {"tool": "#minecraft:pickaxes", "hits": 3, "label": "Pickaxe"}
}
```

Fields: `enabled`, `required_mods`, `source`, `result` or `outputs`, `placed_incomplete`, `title`, `description`, `display_item`, `hud_range`, `materials`, `stages`, `build_time`, `manual_completes`, `accelerators`, `preserve_properties`, `transfer`, `result_data`, `remove_block_on_complete`, `completion_feedback`, and `downgrade`. Select items by registry ID or item tag (`#minecraft:pickaxes`). Stage types are `TOOL_ACTION` and `ITEM_APPLICATION`. Stage and accelerator inputs can be `LEFT`, `RIGHT`, or `BOTH`. Feedback supports `label`, sound, particles, impact, `display_item`, cooldown and completion effects. Any item or tag can be an accelerator: omit `kind` (or use `"kind":"any"`) and set `consume`/`damage` to the desired cost. For example, `{"item":"create:super_glue","actions":1,"reduction":200,"consume":0,"damage":1,"label":"Super Glue"}`. The included JSON files provide working combinations.

### Custom display names

Use `label` to name a material, required action, accelerator, or downgrade tool independently of its item ID or tag. For example, if your pack defines the `#c:hammers` item tag, this action accepts any item in that tag and displays **Hammer** instead of a specific item's name:

```json
"stages": [
  {"type": "TOOL_ACTION", "item": "#c:hammers", "actions": 3,
   "input": "RIGHT", "label": "Hammer"}
]
```

The same property works in a material (`{"item":"#c:ingots/iron","count":2,"label":"Iron Ingot"}`), an accelerator, and a downgrade (`{"tool":"#c:hammers","hits":3,"label":"Hammer"}`). The labels appear in the in-world HUD; JEI uses them for action tooltips and overflow entries, while visible item slots retain the item's normal tooltip. `label` changes presentation only: the item or tag still determines what is accepted. The referenced tag must exist in your pack.

`transfer` supports `none`, `copy_data`, `copy_inventory`, and `sophisticated_storage`. With `none`, nonempty source inventories block conversion. `copy_data` is for block entities that understand the same saved data, `copy_inventory` maps source slots to target slots without copying processing timers, and `sophisticated_storage` handles capacity and contents of compatible Sophisticated tiers. Source inventories are restored on failed conversion. `result_data` is a JSON object merged into the new block entity after transfer. It cannot replace `id`, `x`, `y`, or `z`.

For example, to set Sophisticated Storage wood through the same generic system used for any block entity:

```json
"result_data": {"woodType": "spruce"}
```

Without `result_data`, the original wood is preserved. Controller-linked storage must still be unlinked before upgrading.

## KubeJS

Declare recipes at top level in `kubejs/server_scripts/`. Scripts take priority over datapacks and can disable a bundled route with `BlockUpgrades.disable('jco_upgrades:furnace_to_smoker')`. KubeJS is optional.

```js
BlockUpgrades.create('pack:chest_to_copper', upgrade => {
  upgrade.block('sophisticatedstorage:chest')
    .result('sophisticatedstorage:copper_chest')
    .title('Upgrade to Copper Chest')
    .material('minecraft:copper_ingot', 8)
    .transferMode('sophisticated_storage')
    .downgrade('#minecraft:axes', 3)
})
```

KubeJS uses the same display override through the feedback callback, for example `.work('#c:hammers', 3, feedback => feedback.label('Hammer').input('RIGHT'))` or `.material('#c:ingots/iron', 2, feedback => feedback.label('Iron Ingot'))`. For downgrade, use `.downgrade('#c:hammers', 3, 'Hammer')`.

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

Build with Java 21 using `gradlew :upgrades:build` in the development workspace. The distribution JAR is `upgrades/build/libs/block-upgrader-<version>.jar`. Verification profiles are separate and are not required at runtime. Source code is licensed under MIT; see `LICENSE`.
