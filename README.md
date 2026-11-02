# Custom Commodities

In-game interface for defining new commodities. Adds one campaign ability that opens a form;
each saved entry is appended to `data/campaign/commodities.csv` and registered immediately, so
the commodity exists in the running session and is loaded normally from the file on the next
launch.

Built and tested against **Starsector 0.98a-RC8**.

## Use

1. Load a game. The mod grants the ability on load (also on a new game), so it shows up in the
   campaign ability bar next to Transponder / Go Dark.
2. Press **Add Custom Commodity**.
3. Fill the form and press **Save**. A message confirms the id that was written; the commodity is
   immediately usable (cargo, markets, industries).
4. Quit and restart to have it loaded from the CSV by the vanilla loader.

Blank `id` is generated from the name (`Mesa Spice` → `mesa_spice`). An id that already exists
(a core commodity, another mod, or one you made earlier) gets a `_2`, `_3`, … suffix rather than
overwriting anything.

## Parameters

Every row is written in the core column order of `data/campaign/commodities.csv` (23 columns).

| Form field | CSV column | Default |
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
| Price variability | `price variability` | V4 |
| Economy tier | `economyTier` | 1 |
| Econ unit | `econUnit` | 300 |
| Base raid danger | `baseRaidDanger` | LOW |
| Order | `order` | 100 |
| Icon path | `icon` | `graphics/cc_items/test1.png` |
| Sound id | `sound id` / `sound id drop` | `ui_cargo_food` |

Not exposed, written empty: `origin`, `econUnitOrigUnused`, `eU * base value`, `plugin`, `desc`.

## Caveats

- A commodity only has a **price** if something supplies and demands it. A brand new good with no
  producing industry and no demand reads 0 at markets; give it a demand sink (an industry demand
  or a population-industry demand with a namespaced modifier key) to make it tradeable.
- Codex text is not written: `data/strings/descriptions.csv` is a separate file. Commodities
  defined here have a name and icon but no codex description.
- The write is atomic (tmp file + move) and is re-read after writing; if the write fails, nothing
  is registered, so a save can never reference a commodity whose row is missing.
- Definitions live in the mod folder, so they are global across campaigns and do not travel with
  a save file.

## Build

```
build.bat
```

Requires a JDK 17 (the game runs on Java 17). The build compiles `src/` against
`starsector-core/starfarer.api.jar` + `log4j-1.2.9.jar` and packs `jars/CustomCommodities.jar`.
Close the game first — the jar is locked while it runs.

## How the runtime registration works (0.98a)

The game builds its spec table once at startup and exposes no API to add a commodity later, so
the mod reaches into the obfuscated loader:

- spec implementation: `com.fs.starfarer.loading.OOo` + 253 × `O`, public constructor
  `(String id, String name, float basePrice)`, setters inherited from `com.fs.starfarer.loading.while`
  (`setEconUnit`, `setUtility`, `setDemandClass`, `addTag`, …);
- registration: `com.fs.starfarer.loading.SpecStore.o00000(Class, String, Object)` — a map put;
  `SettingsAPI.getCommoditySpec(id)` reads the same map.

Both names are version specific. If the game updates, re-check them in `starfarer_obf.jar`
(`javap -p -c`) before trusting the mod.

Market-side data needs no backfill: `Market.getCommodityData(id)` creates the `CommodityOnMarket`
on first access, and `MarketDemandData.getDemand(class)` does the same for demand classes.

## Log lines

Startup:

```
[CC] Custom Commodities loaded. Commodity file: ...\mods\Custom Commodities\data\campaign\commodities.csv
Loaded spec with id [cc_add_commodity]
Loaded spec with id [<your commodity id>]      <- after a restart
```

On save: `[CC] wrote '<id>' to <path>` and `[CC] registered commodity <id> ...`.
