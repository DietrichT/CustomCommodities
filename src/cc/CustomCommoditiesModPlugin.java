package cc;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CustomCommoditiesModPlugin extends BaseModPlugin {

    public static final String ABILITY_ID = "cc_add_commodity";

    private static final Logger LOG = Global.getLogger(CustomCommoditiesModPlugin.class);

    /**
     * Register the stored commodities before any save is deserialised: a save that contains
     * CommodityOnMarket objects for these ids re-resolves the spec by id while loading, so the
     * spec table has to be filled in first.
     */
    @Override
    public void onApplicationLoad() {
        try {
            List<CommodityParams> stored = CommodityStore.loadAll();
            int registered = 0;
            for (CommodityParams p : stored) {
                if (Global.getSettings().getCommoditySpec(p.id) != null) continue;
                CommoditySpecs.register(p);
                registered++;
            }
            LOG.info("[CC] Custom Commodities: " + stored.size() + " stored definition(s), "
                    + registered + " registered this session (" + CommodityStore.FILE + ")");
        } catch (Exception e) {
            LOG.error("[CC] could not register stored commodities", e);
        }
    }

    @Override
    public void onNewGame() {
        ensureAbility();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        ensureAbility();
        // Cheap idempotent re-check: covers a dev-mode reload that rebuilds the spec table.
        try {
            List<String> missing = new ArrayList<String>();
            for (CommodityParams p : CommodityStore.loadAll()) {
                if (Global.getSettings().getCommoditySpec(p.id) == null) {
                    CommoditySpecs.register(p);
                    missing.add(p.id);
                }
            }
            if (!missing.isEmpty()) {
                LOG.warn("[CC] re-registered commodities missing from the spec table: " + missing);
            }
        } catch (Exception e) {
            LOG.error("[CC] re-register check failed", e);
        }
    }

    private void ensureAbility() {
        try {
            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
            if (fleet == null) return;
            if (!fleet.hasAbility(ABILITY_ID)) {
                fleet.addAbility(ABILITY_ID);
                LOG.info("[CC] granted ability " + ABILITY_ID);
            }
        } catch (Exception e) {
            LOG.error("[CC] could not grant " + ABILITY_ID, e);
        }
    }
}
