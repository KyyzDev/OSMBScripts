package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Tab;
import utils.Task;

import java.util.Collections;
import java.util.Set;

import static main.KyyzMasterFarmer.*;

public class EatTask extends Task {

    // Common food IDs
    private static final Set<Integer> FOOD_IDS = Set.of(
        379,  // Lobster
        385,  // Shark
        7946, // Monkfish
        329,  // Salmon
        373,  // Swordfish
        2142, // Cooked karambwan
        361,  // Tuna
        333,  // Trout
        315   // Shrimp
    );

    public EatTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone || !eatFood) {
            return false;
        }

        Integer currentHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();
        if (currentHp == null) {
            return false;
        }

        return currentHp <= hpThreshold;
    }

    @Override
    public boolean execute() {
        // Safety check - should never execute if eatFood is false or foodItemId is invalid
        if (!eatFood || foodItemId <= 0) {
            script.log("WARN", "EatTask executed with NO_FOOD mode - this shouldn't happen!");
            return false;
        }

        task = "Eating food";
        Integer currentHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();
        script.log("INFO", "!!! EMERGENCY - HP at " + currentHp + " (threshold: " + hpThreshold + ") - MUST EAT NOW !!!");

        // CRITICAL: Open inventory tab immediately
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        script.submitTask(() -> false, 200);  // Small delay to ensure inventory opens

        // Search for our specific food item by ID
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Set.of(foodItemId));
        if (inv == null) {
            script.log("WARN", "Inventory not visible - trying again...");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
            script.submitTask(() -> false, 300);
            return false;
        }

        // Check if we have ANY food at all - if not, go to bank immediately!
        if (!inv.contains(foodItemId)) {
            script.log("WARN", "!!! CRITICAL - NO FOOD IN INVENTORY! HP is at " + currentHp + " - GOING TO BANK IMMEDIATELY !!!");
            needFood = true;
            return false;  // Let BankTask activate
        }

        // We have food - eat it IMMEDIATELY!
        script.log("INFO", "Found " + foodName + " in inventory - EATING NOW...");
        ItemSearchResult foodItem = inv.getItem(java.util.Set.of(foodItemId));
        if (foodItem == null) {
            script.log("WARN", "Could not get food item - trying again...");
            return false;
        }

        String itemName = script.getItemManager().getItemName(foodItem.getId());
        script.log("INFO", "!!! Eating " + itemName + " (ID: " + foodItem.getId() + ") !!!");

        Integer beforeHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();

        // Interact with food to eat it
        if (!foodItem.interact("Eat")) {
            script.log("WARN", "Failed to interact with food item - RETRYING!");
            script.submitTask(() -> false, 100);
            return false;
        }

        // WAIT LONGER for HP to increase before continuing (CRITICAL for survival!)
        script.log("INFO", "Waiting for food to be consumed and HP to increase...");
        boolean hpIncreased = script.submitTask(() -> {
            Integer afterHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();
            if (afterHp != null && beforeHp != null && afterHp > beforeHp) {
                script.log("INFO", "!!! SUCCESS - Food consumed! HP increased from " + beforeHp + " to " + afterHp + " !!!");
                return true;
            }
            return false;
        }, 5000);  // Increased from 3000 to 5000ms

        if (!hpIncreased) {
            script.log("WARN", "!!! WARNING - HP did not increase after eating - trying to eat again! !!!");
            return false;  // Try eating again
        }

        // Add LONGER safety delay after eating before resuming pickpocketing
        script.log("INFO", "Food eaten successfully - waiting before resuming...");
        script.submitHumanTask(() -> false, script.random(800, 1500));

        return true;  // Return true to indicate we handled eating
    }
}
