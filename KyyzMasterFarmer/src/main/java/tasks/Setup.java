package tasks;

import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Tab;
import utils.Task;

import static main.KyyzMasterFarmer.*;

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
        script.log("INFO", "");
        script.log("INFO", "========================================");
        script.log("INFO", "    Kyyz Thiever v1.0");
        script.log("INFO", "========================================");
        script.log("INFO", "");
        script.log("INFO", "Configuration:");
        script.log("INFO", "  Target NPC: " + targetNPC);
        script.log("INFO", "  Eat food: " + (eatFood ? "Yes at " + hpThreshold + " HP" : "No"));
        script.log("INFO", "  Loot mode: " + lootMode);
        script.log("INFO", "");
        script.log("WARN", "===============================================================");
        script.log("WARN", "  IMPORTANT: TAG YOUR TARGET NPC WITH CYAN HIGHLIGHT!        ");
        script.log("WARN", "===============================================================");
        script.log("WARN", "");
        script.log("WARN", "Steps to highlight:");
        script.log("WARN", "  1. Go to Settings -> NPC highlighting");
        script.log("WARN", "  2. Set color to #05F8F8 (cyan)");
        script.log("WARN", "  3. Use gnome button (hotkeys menu) to tag " + targetNPC);
        script.log("WARN", "  4. You should see CYAN highlight on " + targetNPC);
        script.log("WARN", "");
        script.log("INFO", "Script will ONLY click highlighted NPCs!");
        script.log("INFO", "");

        // Open inventory tab
        task = "Open inventory tab";
        script.log("INFO", "Opening inventory tab...");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check if we have food in inventory (if eating is enabled and foodItemId is valid)
        if (eatFood && foodItemId > 0) {
            script.log("INFO", "Checking for food in inventory...");
            com.osmb.api.item.ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Set.of(foodItemId));

            if (inv == null || !inv.contains(foodItemId)) {
                script.log("INFO", "No food found in inventory - will get food from bank");
                needFood = true;
            } else {
                int foodCount = inv.getAmount(foodItemId);
                script.log("INFO", "Found " + foodCount + " " + foodName + " in inventory - good to go!");
            }
        } else if (!eatFood) {
            script.log("INFO", "No food mode enabled - skipping food check");
        }

        setupDone = true;
        return true;
    }
}
