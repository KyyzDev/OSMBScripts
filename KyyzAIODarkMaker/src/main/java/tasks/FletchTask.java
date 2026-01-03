package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static main.KyyzDartMaker.*;

public class FletchTask extends Task {

    private int previousTipCount = -1;
    private int batchesMade = 0;

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
        script.submitTask(() -> false, script.random(100, 200));

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

        if (!tipItem.interact()) {
            return true;
        }
        script.submitTask(() -> false, script.random(80, 150));

        if (!primaryItem.interact()) {
            script.getWidgetManager().getInventory().unSelectItemIfSelected();
            return true;
        }

        boolean dialogueOpened = script.submitHumanTask(() -> {
            DialogueType dt = script.getWidgetManager().getDialogue().getDialogueType();
            return dt == DialogueType.ITEM_OPTION;
        }, 3000);

        if (dialogueOpened) {
            handleDialogue();
        } else {
            script.getWidgetManager().getInventory().unSelectItemIfSelected();
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

        if (!selected) {
            return;
        }

        task = "Making darts";
        waitUntilFinishedProducing();
    }

    private void waitUntilFinishedProducing() {
        AtomicInteger lastTipCount = new AtomicInteger(-1);
        Timer amountChangeTimer = new Timer();
        int amountChangeTimeout = script.random(3500, 6000);

        script.submitHumanTask(() -> {
            DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.submitTask(() -> false, script.random(800, 2500));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                return true;
            }

            ItemGroupResult inv = script.getWidgetManager().getInventory()
                    .search(Set.of(primaryIngredientID, selectedDartTipID));

            if (inv == null) {
                return false;
            }

            if (!inv.contains(primaryIngredientID) || !inv.contains(selectedDartTipID)) {
                if (previousTipCount > 0 && lastTipCount.get() >= 0) {
                    int made = previousTipCount - lastTipCount.get();
                    if (made > 0) {
                        dartsMade += made;
                        batchesMade++;
                    }
                }
                return true;
            }

            int currentTipCount = inv.getAmount(selectedDartTipID);
            if (lastTipCount.get() == -1) {
                lastTipCount.set(currentTipCount);
                previousTipCount = currentTipCount;
                amountChangeTimer.reset();
            } else if (currentTipCount < lastTipCount.get()) {
                int made = lastTipCount.get() - currentTipCount;
                dartsMade += made;
                lastTipCount.set(currentTipCount);
                amountChangeTimer.reset();
            }

            return false;
        }, 60000, false, true);

        batchesMade++;
    }
}
