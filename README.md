# Custom Commodities

In-game interface for defining new commodities: a campaign ability opens a form, the values are
stored as JSON and the commodity is registered immediately. No restart needed — the mod re-registers
everything at startup.

Built and tested against **Starsector 0.98a-RC8**.

## Use

1. Load a game. The mod grants the ability on load (also on a new game); it sits in the campaign
   ability bar next to Transponder / Go Dark.
2. Press **Add Custom Commodity**.
3. Fill the form and press **Save**. A message confirms the id; the commodity exists from that
   moment (cargo, markets, industries) and every launch afterwards.

Blank `id` is generated from the name (`Mesa Spice` → `mesa_spice`). An id that already exists
(a core commodity, another mod, or one you made earlier) gets a `_2`, `_3`, … suffix rather than
overwriting anything.

## Where the data lives

`saves/common/customcommodities_commodities.json.data` — written through
`Global.getSettings().writeJSONToCommon(...)`, which is the only file access a mod is allowed
(see "Sandbox" below). That folder is shared by all campaigns, so definitions are global and do
not travel with a save file. The game documents this folder as disposable: if it is deleted, saves
that contain the commodity come back with a missing spec, so keep a copy if you care.

## Parameters

| Form field | Maps to | Default |
|---|---|---|
| Name | `name` | — |
| Id | `id` | slug of the name |
| Base price | `base price` | 60 |
| Export value | `export value` | 1.5 × base price |
| Utility | `utility` | 1 (normal trade good; 0 breaks price maths) |
| Tags | `tags` | empty (`military`, `food`, `luxury`, …) |
| Stack size | `stack size` | 1000 |
| Cargo space | `cargo space` | 1 |
| Demand class | `demand class` | the id |
| Price variability | `price variability` | V4 (V0–V10) |
| Economy tier | `economyTier` | 1 |
| Econ unit | `econUnit` | 300 |
| Base raid danger | `baseRaidDanger` | LOW |
| Order | `order` | 100 |
| Icon path | `icon` (80×80 png) | `graphics/cc_items/test1.png` |
| Sound id | `sound id` / `sound id drop` | `ui_cargo_food` |

Not exposed, left at engine defaults: `origin`, `econUnitOrigUnused`, `eU * base value`,
`plugin`, `desc`.

## Caveats

- A commodity only has a **price** if something supplies and demands it. A brand new good with no
  producing industry and no demand reads 0 at markets; give it a demand sink (an industry demand,
  or a population-industry demand with a namespaced modifier key) to make it tradeable.
- No codex text: `data/strings/descriptions.csv` is a separate file that a mod cannot write, so
  these commodities have a name and icon but no description.
- The write is committed before the spec is registered; if the write fails, nothing is registered.

## Build

```
build.bat
```

JDK 17. Compiles `src/` against `starfarer.api.jar`, `starfarer_obf.jar` (obfuscated game classes),
`json.jar` and `log4j-1.2.9.jar`, then packs `jars/CustomCommodities.jar`. Close the game first —
the jar is locked while it runs.

## Sandbox: why the CSV route was dropped, and how registration works

Mod code is loaded by `com.fs.starfarer.loading.scripts.B`, a class loader that refuses to resolve
`java.io` / `java.nio.file` classes and `java.lang.reflect` for mods. Attempting it throws:

```
java.lang.SecurityException: File access and reflection are not allowed to scripts.
```

So writing `data/campaign/commodities.csv` from inside the game is impossible, and so is
reflection. The two workarounds used here:

- **Persistence** — `SettingsAPI.fileExistsInCommon` / `readJSONFromCommon` / `writeJSONToCommon`,
  the same pattern as Starfarer's own `com.fs.starfarer.api.impl.BaseSharedJSONFile`. The game
  performs the IO.
- **Registration** — direct calls instead of reflection. The spec class and its registry are
  public in `starfarer_obf.jar`, so the mod compiles against that jar and calls them normally:
  - spec impl: `com.fs.starfarer.loading.OOo` + 253 × `O` (simple name 256 chars),
    constructor `(String id, String name, float basePrice)`, setters inherited from
    `com.fs.starfarer.loading.while` (`setEconUnit`, `setUtility`, `setDemandClass`, `addTag`, …);
  - registry: `com.fs.starfarer.loading.SpecStore.o00000(Class, String, Object)` — a map put, and
    exactly what `SettingsAPI.getCommoditySpec(id)` reads back.

Both names are version specific. If the game updates, re-check them
(`javap -p -classpath starfarer_obf.jar com.fs.starfarer.loading.SpecStore`) and rebuild.

Market-side data needs no backfill: `Market.getCommodityData(id)` creates the `CommodityOnMarket`
on first access, and `MarketDemandData.getDemand(class)` does the same for demand classes.

## Log lines

Startup:

```
[CC] registered commodity player_xyz 'Xyz' base price 60.0 econ unit 300.0 utility 1.0 demand class player_xyz tags [food]
[CC] Custom Commodities: 1 stored definition(s), 1 registered this session (customcommodities_commodities.json)
Loaded spec with id [cc_add_commodity]
```

On save, after the form is confirmed:

```
[CC] saved 'player_xyz' to customcommodities_commodities.json (1 commodities)
[CC] registered commodity player_xyz 'Xyz' ...
```
