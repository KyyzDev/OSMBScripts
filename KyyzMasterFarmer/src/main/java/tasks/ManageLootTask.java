package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Tab;
import utils.Task;

import java.util.Set;

import static main.KyyzMasterFarmer.*;

public class ManageLootTask extends Task {

    // All seed IDs that Master Farmer can drop
    private static final java.util.Set<Integer> SEED_IDS = java.util.Set.of(
            5291, 5292, 5293, 5294, 5295, 5296, 5297, 5298, 5299, 5300,
            5301, 5302, 5303, 5304, 5305, 5306, 5307, 5308, 5309, 5310,
            5311, 5280, 5281, 5282, 5283, 5284, 5285, 5286, 5287, 5288, 5289, 5290,
            5318, 5319, 5320, 5321, 5322, 5323, 5324
    );

    public ManageLootTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone) {
            return false;
        }

        if (!"DROP_ALL".equals(lootMode)) {
            return false;
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Collections.emptySet());
        if (inv == null) {
            script.log("WARN", "ManageLootTask: inventory check returned null");
            return false;
        }

        int freeSlots = inv.getFreeSlots();
        int usedSlots = 28 - freeSlots;

        return usedSlots >= dropThreshold;
    }

    @Override
    public boolean execute() {
        task = "Dropping loot";

        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        script.submitTask(() -> false, 300);

        java.util.Set<Integer> allItemsToDrop = new java.util.HashSet<>();

        ItemGroupResult seedInv = script.getWidgetManager().getInventory().search(SEED_IDS);
        if (seedInv != null && !seedInv.isEmpty()) {
            java.util.List<com.osmb.api.item.ItemSearchResult> allSeeds = seedInv.getOneOfEachItem();
            for (com.osmb.api.item.ItemSearchResult seed : allSeeds) {
                allItemsToDrop.add(seed.getId());
            }
        }

        if (allItemsToDrop.isEmpty()) {
            return false;
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            script.getWidgetManager().getInventory().dropItems(allItemsToDrop);

            ItemGroupResult afterDrop = script.getWidgetManager().getInventory().search(allItemsToDrop);
            if (afterDrop == null || afterDrop.isEmpty()) {
                break;
            }

            script.submitTask(() -> false, script.random(200, 400));
        }

        dropThreshold = 24 + (int)(Math.random() * 5);

        return false;
    }
}
