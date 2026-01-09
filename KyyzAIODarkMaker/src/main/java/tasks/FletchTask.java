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
        // Proportional scaling: 50% = 2x slower, 10% = 10x slower, 1% = 100x slower
        // At 100%: 20ms, at 50%: 40ms, at 10%: 200ms, at 1%: 2000ms
        int baseDelay = 20;
        return (int) (baseDelay * (100.0 / Math.max(tapSpeed, 1)));
    }

    private int getMaxClickDelay() {
        // Proportional scaling with slightly higher max
        // At 100%: 50ms, at 50%: 100ms, at 10%: 500ms, at 1%: 5000ms
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

        // Use interact(true) to bypass human delays for fast clicking
        if (!tipItem.interact(true)) {
            return true;
        }

        // Anti-ban: small delay between clicks, scales with speed
        // At 100%: 15% chance of tiny 10-30ms delay
        // At lower speeds: always delay based on speed setting
        if (tapSpeed >= 100) {
            // Occasional micro-delay for anti-ban (15% chance)
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

        // Track that we clicked - will count darts when dialogue is handled
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
            // Each dialogue click = 10 darts made (one game action)
            dartsMade += 10;
        }
    }
}
