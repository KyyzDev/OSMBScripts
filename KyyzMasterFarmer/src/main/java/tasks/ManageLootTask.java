package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static main.KyyzMasterFarmer.*;

public class ManageLootTask extends Task {

    // Common food IDs (same as EatTask)
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

    public ManageLootTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone) {
            return false;
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inv == null) {
            return false;
        }

        return inv.getOccupiedSlotCount() >= 26;
    }

    @Override
    public boolean execute() {
        task = "Managing loot";

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inv == null) {
            return false;
        }

        if (lootMode.equals("KEEP_COINS")) {
            return dropExceptCoins(inv);
        } else {
            return dropAllLoot(inv);
        }
    }

    private boolean dropAllLoot(ItemGroupResult inv) {
        script.log("DEBUG", "Dropping all loot...");

        for (int slot = 0; slot < 28; slot++) {
            if (inv.isSlotOccupied(slot)) {
                boolean isFood = false;
                for (var item : inv.getRecognisedItems()) {
                    if (item.getSlot() == slot && FOOD_IDS.contains(item.getId())) {
                        isFood = true;
                        break;
                    }
                }

                if (!isFood) {
                    script.getWidgetManager().getInventory().dropItem(slot, 1, true);
                    script.sleep(script.random(50, 150));
                }
            }
        }

        return true;
    }

    private boolean dropExceptCoins(ItemGroupResult inv) {
        script.log("DEBUG", "Dropping loot, keeping coins...");

        Set<Integer> keepIds = new HashSet<>(FOOD_IDS);
        keepIds.add(ItemID.COINS_995);

        for (int slot = 0; slot < 28; slot++) {
            if (inv.isSlotOccupied(slot)) {
                boolean shouldKeep = false;
                for (var item : inv.getRecognisedItems()) {
                    if (item.getSlot() == slot && keepIds.contains(item.getId())) {
                        shouldKeep = true;
                        break;
                    }
                }

                if (!shouldKeep) {
                    script.getWidgetManager().getInventory().dropItem(slot, 1, true);
                    script.sleep(script.random(50, 150));
                }
            }
        }

        return true;
    }
}
