package cc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.PriceVariability;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Builds a CommoditySpec and puts it into the game's spec table for the current session.
 *
 * The spec class and SpecStore are obfuscated in starfarer_obf.jar:
 *   spec impl : com.fs.starfarer.loading.OOo + "O" * 278  (public ctor (id, name, basePrice))
 *   registry  : com.fs.starfarer.loading.SpecStore.o00000(Class, String, Object) -> map.put(id, spec)
 * Both are version specific; if the game updates, check them again in the obf jar before trusting
 * anything here.
 */
public class CommoditySpecs {

    private static final Logger LOG = Global.getLogger(CommoditySpecs.class);

    private static final String SPEC_CLASS_NAME = "com.fs.starfarer.loading.OOo" + "O".repeat(253);
    private static final String SPEC_STORE_CLASS = "com.fs.starfarer.loading.SpecStore";
    private static final String STORE_PUT = "o00000";

    /** Adds a numeric suffix until the id is unused, so a core or mod commodity is never overwritten. */
    public static String uniqueId(String wanted, List<String> warnings) {
        String id = wanted;
        int n = 2;
        while (Global.getSettings().getCommoditySpec(id) != null) {
            id = wanted + "_" + n;
            n++;
        }
        if (!id.equals(wanted)) {
            warnings.add("id " + wanted + " is already taken, using " + id);
        }
        return id;
    }

    /** Registers the commodity for this session; call only after the CSV row is safely on disk. */
    public static CommoditySpecAPI register(CommodityParams p) throws Exception {
        Class<?> specCls = Class.forName(SPEC_CLASS_NAME);
        Object spec = specCls.getConstructor(String.class, String.class, float.class)
                .newInstance(p.id, p.name, Float.valueOf(p.basePrice));

        set(specCls, spec, "setDemandClass", String.class, p.demandClass);
        set(specCls, spec, "setUtility", float.class, p.utility);
        set(specCls, spec, "setExportValue", float.class, p.exportValue);
        set(specCls, spec, "setPriceVariability", PriceVariability.class, PriceVariability.valueOf(p.priceVariability));
        set(specCls, spec, "setStackSize", int.class, p.stackSize);
        set(specCls, spec, "setCargoSpace", float.class, p.cargoSpace);
        set(specCls, spec, "setIconName", String.class, p.icon);
        set(specCls, spec, "setIconLargeName", String.class, p.icon);
        set(specCls, spec, "setIconWidthMult", float.class, p.iconWidthMult);
        set(specCls, spec, "setOrder", float.class, p.order);
        set(specCls, spec, "setEconomyTier", float.class, p.economyTier);
        set(specCls, spec, "setEconUnit", float.class, p.econUnit);
        set(specCls, spec, "setOrigin", String.class, p.origin);
        set(specCls, spec, "setSoundId", String.class, p.soundId);
        set(specCls, spec, "setSoundIdDrop", String.class, p.soundIdDrop);
        set(specCls, spec, "setBaseDanger", RaidDangerLevel.class, RaidDangerLevel.valueOf(p.baseRaidDanger));
        for (String tag : p.tags) {
            set(specCls, spec, "addTag", String.class, tag);
        }

        Class<?> store = Class.forName(SPEC_STORE_CLASS);
        Method put = store.getMethod(STORE_PUT, Class.class, String.class, Object.class);
        put.invoke(null, specCls, p.id, spec);

        CommoditySpecAPI registered = Global.getSettings().getCommoditySpec(p.id);
        if (registered == null) {
            throw new IllegalStateException("spec table did not accept id " + p.id
                    + " (SpecStore lookup returned null)");
        }
        LOG.info("[CC] registered commodity " + registered.getId() + " '" + registered.getName()
                + "' base price " + registered.getBasePrice() + " econ unit " + registered.getEconUnit()
                + " utility " + registered.getUtility() + " demand class " + registered.getDemandClass()
                + " tags " + registered.getTags());
        return registered;
    }

    private static void set(Class<?> specCls, Object spec, String method, Class<?> type, Object value) throws Exception {
        Method m = specCls.getMethod(method, type);
        m.invoke(spec, value);
    }
}
