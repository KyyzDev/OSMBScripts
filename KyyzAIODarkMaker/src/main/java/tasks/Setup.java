package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Tab;
import utils.Task;

import java.util.Set;

import static main.KyyzDartMaker.*;

public class Setup extends Task {

    public Setup(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return !setupDone;
    }

    @Override
    public boolean execute() {
        task = "Setup";

        script.log("INFO", "Running initial setup...");

        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        script.submitTask(() -> false, 300);

        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(primaryIngredientID, selectedDartTipID));

        if (inv == null) {
            script.log("WARN", "Cannot see inventory!");
            script.stop();
            return false;
        }

        if (!inv.contains(primaryIngredientID)) {
            String itemName = script.getItemManager().getItemName(primaryIngredientID);
            script.log("ERROR", "Missing required item: " + itemName);
            script.log("ERROR", "Please ensure you have both dart tips and " +
                    (primaryIngredientID == 314 ? "feathers" : "headless atlatl darts") +
                    " in your inventory!");
            script.stop();
            return false;
        }

        if (!inv.contains(selectedDartTipID)) {
            String itemName = script.getItemManager().getItemName(selectedDartTipID);
            script.log("ERROR", "Missing required item: " + itemName);
            script.log("ERROR", "Please ensure you have both dart tips and " +
                    (primaryIngredientID == 314 ? "feathers" : "headless atlatl darts") +
                    " in your inventory!");
            script.stop();
            return false;
        }

        int tipCount = inv.getAmount(selectedDartTipID);
        int primaryCount = inv.getAmount(primaryIngredientID);

        script.log("INFO", "Found " + tipCount + "x " + script.getItemManager().getItemName(selectedDartTipID));
        script.log("INFO", "Found " + primaryCount + "x " + script.getItemManager().getItemName(primaryIngredientID));

        int maxDarts = Math.min(tipCount, primaryCount) * 10;
        script.log("INFO", "Can make approximately " + maxDarts + " darts");

        setupDone = true;
        startTime = System.currentTimeMillis();
        script.log("INFO", "Setup complete! Starting dart making...");

        return false;
    }
}
