package cc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;

/**
 * Campaign ability button (same base class as Active Sensor Burst / Sustained Burn).
 * Pressing it opens the commodity form; the ability itself has no effect and expires on its own.
 */
public class AddCommodityAbility extends BaseDurationAbility {

    @Override
    protected void activateImpl() {
        Global.getSector().getCampaignUI().showInteractionDialog(
                new AddCommodityDialogPlugin(), Global.getSector().getPlayerFleet());
    }

    @Override
    protected void applyEffect(float amount, float level) {
    }

    @Override
    protected void deactivateImpl() {
    }

    @Override
    protected void cleanupImpl() {
    }
}
