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

    private int getMinClickDelay() {
        int baseDelay = 20;
        return (int) (baseDelay * (100.0 / Math.max(tapSpeed, 1)));
    }

    private int getMaxClickDelay() {
        int baseDelay = 50;
        return (int) (baseDelay * (100.0 / Math.max(tapSpeed, 1)));
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
        task = "Fletching darts";

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            handleDialogue();
            return true;
        }

        if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
            script.submitTask(() -> false, script.random(800, 1500));
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

        task = "Combining items";

        if (!tipItem.interact(true)) {
            return true;
        }

        if (tapSpeed >= 100) {
            if (script.random(100) < 15) {
                script.submitTask(() -> false, script.random(10, 30));
            }
        } else {
            script.submitTask(() -> false, script.random(getMinClickDelay(), getMaxClickDelay()));
        }

        if (!primaryItem.interact(true)) {
            script.getWidgetManager().getInventory().unSelectItemIfSelected();
            return true;
        }

        return true;
    }

    private void handleDialogue() {
        task = "Selecting dart type";

        boolean selected = script.getWidgetManager().getDialogue()
                .selectItem(resultingDartID, selectedDartTipID);

        if (!selected) {
            selected = script.getWidgetManager().getDialogue().selectItem(resultingDartID);
        }

        if (selected) {
            task = "Making darts";
            dartsMade += 10;
        }
    }
}
