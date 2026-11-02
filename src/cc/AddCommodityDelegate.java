package cc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The form: one text field per commodities.csv parameter, in two columns. */
public class AddCommodityDelegate implements CustomDialogDelegate {

    public static final float WIDTH = 880f;
    public static final float HEIGHT = 560f;
    private static final float COL = 410f;

    private static final Logger LOG = Global.getLogger(AddCommodityDelegate.class);

    private final InteractionDialogAPI dialog;
    private final Map<String, TextFieldAPI> fields = new LinkedHashMap<String, TextFieldAPI>();
    private final Map<String, String> values = new LinkedHashMap<String, String>();

    public AddCommodityDelegate(InteractionDialogAPI dialog) {
        this.dialog = dialog;
    }

    public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
        fields.clear();

        TooltipMakerAPI left = panel.createUIElement(COL, HEIGHT - 16f, false);
        left.addSectionHeading("Commodity", Alignment.MID, 2f);
        left.addPara("Maps to data/campaign/commodities.csv. Blank id is built from the name.", Misc.getGrayColor(), 8f);
        field(left, "name", "Name", "Crystalline Spice");
        field(left, "id", "Id (blank = auto, a-z 0-9 _)", "");
        field(left, "baseprice", "Base price", "60");
        field(left, "export", "Export value", "90");
        field(left, "utility", "Utility (1 = normal trade good)", "1");
        field(left, "tags", "Tags, comma separated (e.g. military, food)", "");
        field(left, "stack", "Stack size", "1000");
        field(left, "cargo", "Cargo space", "1");
        panel.addUIElement(left).inTL(10f, 10f);

        TooltipMakerAPI right = panel.createUIElement(COL, HEIGHT - 16f, false);
        right.addSectionHeading("Economy", Alignment.MID, 2f);
        right.addPara("Blank demand class falls back to the id. Prices still need supply/demand.", Misc.getGrayColor(), 8f);
        field(right, "demandclass", "Demand class (blank = id)", "");
        field(right, "variability", "Price variability (V0 - V10)", "V4");
        field(right, "tier", "Economy tier", "1");
        field(right, "econunit", "Econ unit", "300");
        field(right, "raid", "Base raid danger (NONE, MINIMAL, LOW, MEDIUM, HIGH, EXTREME)", "LOW");
        field(right, "order", "Order", "100");
        field(right, "icon", "Icon path (80x80 png)", CommodityParams.DEFAULT_ICON);
        field(right, "sound", "Sound id", CommodityParams.DEFAULT_SOUND);
        panel.addUIElement(right).inTL(440f, 10f);
    }

    private void field(TooltipMakerAPI tooltip, String key, String label, String def) {
        tooltip.addPara(label, Misc.getGrayColor(), 2f);
        TextFieldAPI tf = tooltip.addTextField(COL - 20f, 1f);
        tf.setText(values.containsKey(key) ? values.get(key) : def);
        tf.setMaxChars(200);
        tf.setUndoOnEscape(false);
        fields.put(key, tf);
    }

    public void customDialogConfirm() {
        for (Map.Entry<String, TextFieldAPI> e : fields.entrySet()) {
            values.put(e.getKey(), e.getValue().getText());
        }

        List<String> warnings = new ArrayList<String>();
        CommodityParams p = CommodityParams.fromInputs(values, warnings);
        try {
            p.id = CommoditySpecs.uniqueId(p.id, warnings);
            // Disk first, spec second: a spec that only exists in memory breaks the next save load.
            CommodityStore.append(p);
            CommoditySpecs.register(p);
            Global.getSector().getCampaignUI().addMessage("Custom commodity added: " + p.describe(),
                    Misc.getPositiveHighlightColor(), p.id, p.name, Misc.getHighlightColor(), Misc.getHighlightColor());
            for (String w : warnings) {
                Global.getSector().getCampaignUI().addMessage("  note: " + w, Misc.getGrayColor());
                LOG.warn("[CC] " + w);
            }
        } catch (Exception ex) {
            LOG.error("[CC] failed to add custom commodity", ex);
            Global.getSector().getCampaignUI().addMessage("Custom commodity failed: " + ex,
                    Misc.getNegativeHighlightColor());
        }
        if (dialog != null) dialog.dismiss();
    }

    public void customDialogCancel() {
        if (dialog != null) dialog.dismiss();
    }

    public boolean hasCancelButton() {
        return true;
    }

    public String getConfirmText() {
        return "Save";
    }

    public String getCancelText() {
        return "Cancel";
    }

    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return null;
    }
}
