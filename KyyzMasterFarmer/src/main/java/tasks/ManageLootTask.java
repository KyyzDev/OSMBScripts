package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Tab;
import utils.Task;

import java.util.Set;

import static main.KyyzMasterFarmer.*;

public class ManageLootTask extends Task {

    private static final Set<Integer> SEED_IDS = Set.of(
        5291, 5292, 5293, 5294, 5295, 5296, 5297,
        5298, 5299, 5300, 5301, 5302, 5303, 5304
    );

    public ManageLootTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone || !"DROP_ALL".equals(lootMode)) {
            return false;
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Collections.emptySet());
        if (inv == null || !inv.isFull()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean execute() {
        task = "Dropping loot";

        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        script.submitTask(() -> false, 300);

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Collections.emptySet());
        if (inv == null) {
            return false;
        }

        Set<Integer> keepItems = new java.util.HashSet<>(SEED_IDS);
        keepItems.add(foodItemId);
        keepItems.add(995);
        if (useSeedBox) {
            keepItems.add(SEED_BOX_ID);
        }

        script.getWidgetManager().getInventory().dropItems(keepItems);
        script.submitTask(() -> false, 600);

        return false;
    }
}
