package cc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.PriceVariability;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.loading.SpecStore;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Puts a commodity into the game's spec table for the current session.
 *
 * Direct calls, not reflection: the script sandbox refuses java.lang.reflect
 * ("File access and reflection are not allowed to scripts"), but the spec class has a public
 * constructor plus public setters, so plain bytecode calls work fine.
 *
 * The names below are obfuscated and version specific (0.98a-RC8):
 *   spec impl : com.fs.starfarer.loading.OOo + 253 x 'O'  (simple name 256 chars, FQN 281)
 *   registry  : com.fs.starfarer.loading.SpecStore.o00000(Class, String, Object)  -> map.put(id, spec)
 * On a game update, re-check both with: javap -p -classpath starfarer_obf.jar com.fs.starfarer.loading.SpecStore
 */
public class CommoditySpecs {

    private static final Logger LOG = Global.getLogger(CommoditySpecs.class);

    /** Appends _2, _3 ... until the id is unused, so a core or other mod's commodity is never replaced. */
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

    /** Registers the commodity for this session; call only after the JSON entry is safely written. */
    public static CommoditySpecAPI register(CommodityParams p) {
        var spec = new com.fs.starfarer.loading.OOoOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO(p.id, p.name, p.basePrice);

        spec.setDemandClass(p.demandClass);
        spec.setUtility(p.utility);
        spec.setExportValue(p.exportValue);
        spec.setPriceVariability(PriceVariability.valueOf(p.priceVariability));
        spec.setStackSize(p.stackSize);
        spec.setCargoSpace(p.cargoSpace);
        spec.setIconName(p.icon);
        spec.setIconLargeName(p.icon);
        spec.setIconWidthMult(p.iconWidthMult);
        spec.setOrder(p.order);
        spec.setEconomyTier(p.economyTier);
        spec.setEconUnit(p.econUnit);
        spec.setOrigin(p.origin);
        spec.setSoundId(p.soundId);
        spec.setSoundIdDrop(p.soundIdDrop);
        spec.setBaseDanger(RaidDangerLevel.valueOf(p.baseRaidDanger));
        for (String tag : p.tags) {
            spec.addTag(tag);
        }

        SpecStore.o00000(spec.getClass(), p.id, spec);

        CommoditySpecAPI registered = Global.getSettings().getCommoditySpec(p.id);
        if (registered == null) {
            throw new IllegalStateException("spec table did not accept id " + p.id);
        }
        LOG.info("[CC] registered commodity " + registered.getId() + " '" + registered.getName()
                + "' base price " + registered.getBasePrice() + " econ unit " + registered.getEconUnit()
                + " utility " + registered.getUtility() + " demand class " + registered.getDemandClass()
                + " tags " + registered.getTags());
        return registered;
    }
}
