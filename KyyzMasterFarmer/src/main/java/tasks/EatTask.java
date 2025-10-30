package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Tab;
import utils.Task;

import static main.KyyzMasterFarmer.*;

public class EatTask extends Task {

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
        
        if (!eatFood || foodItemId <= 0) {
            script.log("WARN", "EatTask executed with NO_FOOD mode - this shouldn't happen!");
            return false;
        }

        task = "Eating food";
        Integer currentHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();
        script.log("INFO", "!!! EMERGENCY - HP at " + currentHp + " (threshold: " + hpThreshold + ") - MUST EAT NOW !!!");

        
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        script.submitTask(() -> false, 200);  

        
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Set.of(foodItemId));
        if (inv == null) {
            script.log("WARN", "Inventory not visible - trying again...");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
            script.submitTask(() -> false, 300);
            return false;
        }

        
        if (!inv.contains(foodItemId)) {
            script.log("WARN", "!!! CRITICAL - NO FOOD IN INVENTORY! HP is at " + currentHp + " - GOING TO BANK IMMEDIATELY !!!");
            needFood = true;
            return false;  
        }

        
        script.log("INFO", "Found " + foodName + " in inventory - EATING NOW...");
        ItemSearchResult foodItem = inv.getItem(java.util.Set.of(foodItemId));
        if (foodItem == null) {
            script.log("WARN", "Could not get food item - trying again...");
            return false;
        }

        String itemName = script.getItemManager().getItemName(foodItem.getId());
        script.log("INFO", "!!! Eating " + itemName + " (ID: " + foodItem.getId() + ") !!!");

        Integer beforeHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();

        
        if (!foodItem.interact("Eat")) {
            script.log("WARN", "Failed to interact with food item - RETRYING!");
            script.submitTask(() -> false, 100);
            return false;
        }

        
        script.log("INFO", "Waiting for food to be consumed and HP to increase...");
        boolean hpIncreased = script.submitTask(() -> {
            Integer afterHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();
            if (afterHp != null && beforeHp != null && afterHp > beforeHp) {
                script.log("INFO", "!!! SUCCESS - Food consumed! HP increased from " + beforeHp + " to " + afterHp + " !!!");
                return true;
            }
            return false;
        }, 5000);  

        if (!hpIncreased) {
            script.log("WARN", "!!! WARNING - HP did not increase after eating - trying to eat again! !!!");
            return false;  
        }

        
        script.log("INFO", "Food eaten successfully - waiting before resuming...");
        script.submitHumanTask(() -> false, script.random(800, 1500));

        return true;  
    }
}
