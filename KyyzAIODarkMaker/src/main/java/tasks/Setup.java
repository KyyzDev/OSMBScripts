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

        script.getWidgetManager().getInventory().unSelectItemIfSelected();
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        boolean inventoryVisible = script.submitHumanTask(() -> {
            return script.getWidgetManager().getInventory().search(Set.of(primaryIngredientID, selectedDartTipID)) != null;
        }, 3000);

        if (!inventoryVisible) {
            return true;
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(primaryIngredientID, selectedDartTipID));

        if (inv == null) {
            return true;
        }

        if (!inv.contains(primaryIngredientID) || !inv.contains(selectedDartTipID)) {
            script.stop();
            return false;
        }

        setupDone = true;
        startTime = System.currentTimeMillis();
        dartsMade = 0;

        return true;
    }
}
