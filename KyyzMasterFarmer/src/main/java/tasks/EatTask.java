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
        if (!setupDone || !eatFood || needFood) {
            return false;
        }

        Integer currentHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();

        if (currentHp == null) {
            return false;
        }

        if (currentHp <= hpThreshold) {
            return true;
        }

        return false;
    }

    @Override
    public boolean execute() {

        if (!eatFood || foodItemId <= 0) {
            return false;
        }

        task = "Eating";

        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        script.submitTask(() -> false, 200);

        java.util.Set<Integer> allFoodIds = getFoodIds(foodItemId);
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(allFoodIds);
        if (inv == null) {
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
            script.submitTask(() -> false, 300);
            return false;
        }

        int foodCount = (inv != null && !inv.isEmpty()) ? inv.getAmount(allFoodIds) : 0;

        if (inv.isEmpty() || foodCount == 0) {
            needFood = true;
            return false;
        }

        int targetHp = Math.min(99, hpThreshold + 15);
        int healAmount = getFoodHealAmount(foodItemId);

        Integer checkHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();
        int hpNeeded = Math.max(0, targetHp - (checkHp != null ? checkHp : hpThreshold));
        int bitesNeeded = Math.max(1, (int) Math.ceil((double) hpNeeded / healAmount));
        int maxEatAttempts = Math.min(3, bitesNeeded);

        for (int i = 0; i < maxEatAttempts; i++) {
            Integer hpAfterBite = script.getWidgetManager().getMinimapOrbs().getHitpoints();
            if (hpAfterBite != null && hpAfterBite >= targetHp) {
                break;
            }

            inv = script.getWidgetManager().getInventory().search(allFoodIds);
            if (inv == null || inv.isEmpty()) {
                if (i == 0) needFood = true;
                break;
            }

            ItemSearchResult foodItem = inv.getItem(allFoodIds);
            if (foodItem == null) {
                break;
            }

            Integer beforeHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();

            if (!foodItem.interact("Eat")) {
                script.submitTask(() -> false, 100);
                continue;
            }

            script.submitTask(() -> {
                Integer afterHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();
                return afterHp != null && beforeHp != null && afterHp > beforeHp;
            }, 3000);

            script.submitTask(() -> false, script.random(400, 700));
        }

        script.submitTask(() -> false, script.random(200, 500));

        return true;
    }
}
