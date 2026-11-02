package cc;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import java.util.HashMap;
import java.util.Map;

/** Empty interaction dialog whose only job is to host the custom form. */
public class AddCommodityDialogPlugin implements InteractionDialogPlugin {

    private InteractionDialogAPI dialog;

    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.setPromptText("Custom commodity");
        dialog.showCustomDialog(AddCommodityDelegate.WIDTH, AddCommodityDelegate.HEIGHT,
                new AddCommodityDelegate(dialog));
    }

    public void optionSelected(String optionText, Object optionData) {
    }

    public void optionMousedOver(String optionText, Object optionData) {
    }

    public void advance(float amount) {
    }

    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    public Object getContext() {
        return dialog;
    }

    public Map<String, MemoryAPI> getMemoryMap() {
        return new HashMap<String, MemoryAPI>();
    }
}
