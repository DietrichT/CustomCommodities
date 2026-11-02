package cc;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.apache.log4j.Logger;

public class CustomCommoditiesModPlugin extends BaseModPlugin {

    public static final String ABILITY_ID = "cc_add_commodity";

    private static final Logger LOG = Global.getLogger(CustomCommoditiesModPlugin.class);

    @Override
    public void onApplicationLoad() {
        try {
            LOG.info("[CC] Custom Commodities loaded. Commodity file: " + CommodityStore.csvPath());
        } catch (Exception e) {
            LOG.error("[CC] could not resolve the commodity file path", e);
        }
    }

    @Override
    public void onNewGame() {
        ensureAbility();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        ensureAbility();
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
