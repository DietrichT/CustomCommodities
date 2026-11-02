package cc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

/**
 * One custom commodity: the values the player typed, plus the mapping to the JSON entry kept in the
 * game's common folder. A mod cannot write data/campaign/commodities.csv (the script sandbox blocks
 * java.io), so the stored format is JSON and the spec table is filled in by CommoditySpecs.
 */
public class CommodityParams {

    public static final String DEFAULT_ICON = "graphics/cc_items/test1.png";
    public static final String DEFAULT_SOUND = "ui_cargo_food";

    public static final String[] KEYS = {
            "name", "id", "baseprice", "export", "utility", "tags", "stack", "cargo",
            "demandclass", "variability", "tier", "econunit", "raid", "order", "icon", "sound"
    };

    public String name = "";
    public String id = "";
    public String demandClass = "";
    public float basePrice = 50f;
    public float exportValue = 75f;
    public String priceVariability = "V4";
    public float utility = 1f;
    public String origin = "";
    public List<String> tags = new ArrayList<String>();
    public int stackSize = 1000;
    public float cargoSpace = 1f;
    public String icon = DEFAULT_ICON;
    public String soundId = DEFAULT_SOUND;
    public String soundIdDrop = DEFAULT_SOUND;
    public float order = 100f;
    public float economyTier = 1f;
    public float econUnit = 300f;
    public String baseRaidDanger = "LOW";
    public float iconWidthMult = 1f;

    /** Builds params from the dialog fields; anything unparsable falls back to a default and is reported. */
    public static CommodityParams fromInputs(Map<String, String> in, List<String> warnings) {
        CommodityParams p = new CommodityParams();

        p.name = text(in, "name");
        if (p.name.isEmpty()) {
            p.name = "Custom Commodity";
            warnings.add("name was blank, using " + p.name);
        }

        String rawId = text(in, "id").toLowerCase(Locale.ROOT);
        p.id = rawId.isEmpty() ? slug(p.name) : rawId;
        if (!p.id.matches("[a-z0-9_]+")) {
            String fixed = slug(p.id);
            warnings.add("id sanitised to " + fixed);
            p.id = fixed;
        }
        if (p.id.isEmpty()) p.id = "player_commodity";

        p.demandClass = text(in, "demandclass");
        if (p.demandClass.isEmpty()) p.demandClass = p.id;

        p.basePrice = num(in, "baseprice", 50f, "base price", warnings);
        p.exportValue = num(in, "export", p.basePrice * 1.5f, "export value", warnings);
        p.utility = num(in, "utility", 1f, "utility", warnings);
        p.tags = splitTags(text(in, "tags"));
        p.stackSize = (int) num(in, "stack", 1000f, "stack size", warnings);
        p.cargoSpace = num(in, "cargo", 1f, "cargo space", warnings);
        p.order = num(in, "order", 100f, "order", warnings);
        p.economyTier = num(in, "tier", 1f, "economy tier", warnings);
        p.econUnit = num(in, "econunit", 300f, "econ unit", warnings);

        p.icon = text(in, "icon").replace('\\', '/');
        if (p.icon.isEmpty()) p.icon = DEFAULT_ICON;

        p.soundId = text(in, "sound");
        if (p.soundId.isEmpty()) p.soundId = DEFAULT_SOUND;
        p.soundIdDrop = p.soundId;

        p.priceVariability = parseVariability(text(in, "variability"), warnings);
        p.baseRaidDanger = parseDanger(text(in, "raid"), warnings);

        return p;
    }

    public static CommodityParams fromJSON(JSONObject o, List<String> warnings) {
        Map<String, String> in = new LinkedHashMap<String, String>();
        for (String key : KEYS) {
            in.put(key, o.optString(key, ""));
        }
        return fromInputs(in, warnings);
    }

    /** Every field as text, so the dialog, the stored JSON and the defaults all go through one path. */
    public Map<String, String> toInputs() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("name", name);
        m.put("id", id);
        m.put("baseprice", num(basePrice));
        m.put("export", num(exportValue));
        m.put("utility", num(utility));
        m.put("tags", joinTags(tags));
        m.put("stack", String.valueOf(stackSize));
        m.put("cargo", num(cargoSpace));
        m.put("demandclass", demandClass);
        m.put("variability", priceVariability);
        m.put("tier", num(economyTier));
        m.put("econunit", num(econUnit));
        m.put("raid", baseRaidDanger);
        m.put("order", num(order));
        m.put("icon", icon);
        m.put("sound", soundId);
        return m;
    }

    public JSONObject toJSON() throws org.json.JSONException {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, String> e : toInputs().entrySet()) {
            o.put(e.getKey(), e.getValue());
        }
        return o;
    }

    public String describe() {
        return name + " [" + id + "], base price " + num(basePrice) + ", econ unit " + num(econUnit)
                + ", demand class " + demandClass;
    }

    // ---------------------------------------------------------------- helpers

    private static String text(Map<String, String> in, String key) {
        String v = in.get(key);
        return v == null ? "" : v.trim();
    }

    private static float num(Map<String, String> in, String key, float def, String label, List<String> warnings) {
        String raw = text(in, key);
        if (raw.isEmpty()) return def;
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            warnings.add(label + " '" + raw + "' is not a number, using " + num(def));
            return def;
        }
    }

    private static String parseVariability(String raw, List<String> warnings) {
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if (v.isEmpty()) return "V4";
        if (v.matches("\\d+")) v = "V" + v;
        try {
            com.fs.starfarer.api.campaign.econ.PriceVariability.valueOf(v);
            return v;
        } catch (IllegalArgumentException e) {
            warnings.add("price variability '" + raw + "' is not V0-V10, using V4");
            return "V4";
        }
    }

    private static String parseDanger(String raw, List<String> warnings) {
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if (v.isEmpty()) return "LOW";
        try {
            com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel.valueOf(v);
            return v;
        } catch (IllegalArgumentException e) {
            warnings.add("raid danger '" + raw + "' is not NONE/MINIMAL/LOW/MEDIUM/HIGH/EXTREME, using LOW");
            return "LOW";
        }
    }

    private static List<String> splitTags(String raw) {
        List<String> out = new ArrayList<String>();
        if (raw.isEmpty()) return out;
        for (String part : raw.split("[,;]")) {
            String t = part.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    private static String joinTags(List<String> tags) {
        StringBuilder sb = new StringBuilder();
        for (String t : tags) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(t);
        }
        return sb.toString();
    }

    private static String slug(String raw) {
        return raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String num(float v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}
