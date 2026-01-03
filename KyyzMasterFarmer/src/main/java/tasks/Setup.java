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

        try {
            com.osmb.api.ui.chatbox.Chatbox chatbox = script.getWidgetManager().getChatbox();
            if (chatbox != null && !chatbox.isOpen()) {
                chatbox.open();
                script.submitTask(() -> false, 500);
            }
        } catch (Exception e) {
        }

        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        if (eatFood && foodItemId > 0) {
            java.util.Set<Integer> allFoodIds = getFoodIds(foodItemId);
            com.osmb.api.item.ItemGroupResult inv = script.getWidgetManager().getInventory().search(allFoodIds);
            if (inv == null || inv.isEmpty()) {
                needFood = true;
            }
        }

        if (useSeedBox) {
            com.osmb.api.item.ItemGroupResult inv = script.getWidgetManager().getInventory().search(SEED_BOX_IDS);
            if (inv == null || inv.isEmpty()) {
                needSeedBox = true;
            }
        }

        setupDone = true;
        return true;
    }
}
