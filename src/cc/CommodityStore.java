package cc;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistence for custom commodities.
 *
 * Mod code runs inside Starsector's script sandbox: java.io / java.nio.file and java.lang.reflect
 * are refused ("File access and reflection are not allowed to scripts"), so a mod cannot write its
 * own data folder. The supported door is the settings API, which writes JSON to the game's common
 * folder (shared across saves):
 *
 *   Global.getSettings().fileExistsInCommon(name)
 *   Global.getSettings().readJSONFromCommon(name, cache)
 *   Global.getSettings().writeJSONToCommon(name, json, onlyIfChanged)
 *
 * Starfarer's own com.fs.starfarer.api.impl.BaseSharedJSONFile uses exactly this pattern.
 */
public class CommodityStore {

    public static final String FILE = "customcommodities_commodities.json";

    private static final String KEY = "commodities";
    private static final String VERSION_KEY = "version";
    private static final int VERSION = 1;

    private static final Logger LOG = Global.getLogger(CommodityStore.class);

    private CommodityStore() {
    }

    /** All stored commodities, in file order. Never throws: a broken file just yields an empty list. */
    public static List<CommodityParams> loadAll() {
        List<CommodityParams> out = new ArrayList<CommodityParams>();
        JSONArray arr = read().optJSONArray(KEY);
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            List<String> warnings = new ArrayList<String>();
            CommodityParams p = CommodityParams.fromJSON(o, warnings);
            for (String w : warnings) LOG.warn("[CC] " + p.id + ": " + w);
            out.add(p);
        }
        return out;
    }

    /** Adds or replaces one entry, writes the file synchronously, then reads it back. */
    public static void save(CommodityParams p) throws Exception {
        JSONObject root = read();
        JSONArray current = root.optJSONArray(KEY);
        JSONArray updated = new JSONArray();
        boolean replaced = false;
        if (current != null) {
            for (int i = 0; i < current.length(); i++) {
                JSONObject o = current.optJSONObject(i);
                if (o == null) continue;
                if (p.id.equals(o.optString("id"))) {
                    updated.put(p.toJSON());
                    replaced = true;
                } else {
                    updated.put(o);
                }
            }
        }
        if (!replaced) updated.put(p.toJSON());
        root.put(VERSION_KEY, VERSION);
        root.put(KEY, updated);

        // onlyIfChanged = false: write on this thread, because the spec is registered right after.
        Global.getSettings().writeJSONToCommon(FILE, root, false);

        // Verify what we just built. Do NOT judge success by a read-back: writeJSONToCommon(...,
        // false) does not refresh the settings API's write cache, so a cached read can still
        // return the previous file contents (that is also why read() passes cache = false).
        boolean found = false;
        for (int i = 0; i < updated.length(); i++) {
            JSONObject o = updated.optJSONObject(i);
            if (o != null && p.id.equals(o.optString("id"))) found = true;
        }
        if (!found) {
            throw new IllegalStateException("built JSON for " + FILE + " does not contain " + p.id);
        }

        JSONArray disk = read().optJSONArray(KEY);
        int count = disk == null ? 0 : disk.length();
        LOG.info("[CC] saved '" + p.id + "' to " + FILE + " (" + count + " commodities)");
    }

    private static JSONObject read() {
        JSONObject json = null;
        try {
            if (Global.getSettings().fileExistsInCommon(FILE)) {
                // cache = false: always read the file. A cached copy goes stale after
                // writeJSONToCommon(..., false), which would silently drop the previous entry
                // on the next save in the same session.
                json = Global.getSettings().readJSONFromCommon(FILE, false);
            }
        } catch (Exception e) {
            LOG.warn("[CC] could not read " + FILE + ", starting empty", e);
        }
        if (json == null) json = new JSONObject();
        if (json.optJSONArray(KEY) == null) {
            try {
                json.put(KEY, new JSONArray());
            } catch (Exception e) {
                LOG.warn("[CC] could not initialise " + KEY, e);
            }
        }
        return json;
    }
}
