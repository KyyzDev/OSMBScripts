package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import utils.Task;

import java.util.Set;

import static main.KyyzDartMaker.*;

public class FletchTask extends Task {

    public FletchTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone) {
            return false;
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(primaryIngredientID, selectedDartTipID));

        if (inv == null) {
            return false;
        }

        return inv.contains(primaryIngredientID) && inv.contains(selectedDartTipID);
    }

    @Override
    public boolean execute() {
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();

        if (dialogueType == DialogueType.ITEM_OPTION) {
            task = "Selecting dart";
            script.getWidgetManager().getDialogue().selectItem(resultingDartID, selectedDartTipID);
            task = "Making darts";
            dartsMade += 10;
            return true;
        }

        if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
            return true;
        }

        script.getWidgetManager().getInventory().unSelectItemIfSelected();

        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(primaryIngredientID, selectedDartTipID));

        if (inv == null) {
            return true;
        }

        if (!inv.contains(primaryIngredientID) || !inv.contains(selectedDartTipID)) {
            script.stop();
            return false;
        }

        ItemSearchResult tipItem = inv.getRandomItem(selectedDartTipID);
        ItemSearchResult primaryItem = inv.getRandomItem(primaryIngredientID);

        if (tipItem == null || primaryItem == null) {
            return true;
        }

        task = "Combining";

        if (!tipItem.interact(true)) {
            return true;
        }

        if (tapSpeed < 100) {
            int delay = (int) (30 * (100.0 / Math.max(tapSpeed, 1)));
            script.submitTask(() -> false, script.random(delay / 2, delay));
        }

        if (!primaryItem.interact(true)) {
            script.getWidgetManager().getInventory().unSelectItemIfSelected();
            return true;
        }

        task = "Making darts";
        dartsMade += 10;

        return true;
    }
}
