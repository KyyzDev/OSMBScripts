package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;
import utils.Task;

import java.lang.reflect.Method;
import java.util.Set;

import static main.KyyzDartMaker.*;

public class FletchTask extends Task {

    private enum Phase {
        IDLE,
        FIRST_CLICK,
        WAIT_MIN,
        SECOND_CLICK,
        COOLDOWN
    }

    private Phase phase = Phase.IDLE;
    private boolean firstIsPrimary = true;
    private long nextAtMs = 0;
    private int minDelayMs = 20;

    private static volatile boolean bypassChecked = false;
    private static volatile Method interactBypass = null;

    private long pairsDone = 0;

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

        boolean hasSupplies = inv.contains(primaryIngredientID) && inv.contains(selectedDartTipID);
        boolean inProgress = phase != Phase.IDLE;

        return hasSupplies || inProgress;
    }

    @Override
    public boolean execute() {
        task = "Fletching darts";

        if (System.currentTimeMillis() < nextAtMs) {
            long waitTime = nextAtMs - System.currentTimeMillis();
            script.submitTask(() -> false, (int) Math.max(1, waitTime));
            return true;
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(primaryIngredientID, selectedDartTipID));

        if (inv == null) {
            return true;
        }

        if (!bypassChecked) {
            try {
                var probe = inv.getRandomItem(primaryIngredientID);
                if (probe != null) {
                    interactBypass = probe.getClass().getMethod("interact", boolean.class);
                }
            } catch (Throwable ignored) {
            }
            bypassChecked = true;
        }

        switch (phase) {
            case IDLE:
                phase = Phase.FIRST_CLICK;
                return execute();

            case FIRST_CLICK:
                final int firstId = firstIsPrimary ? primaryIngredientID : selectedDartTipID;

                if (!clickInventoryItem(inv, firstId, true) &&
                        !clickInventoryItem(inv, firstId, false)) {
                    script.getWidgetManager().getInventory().unSelectItemIfSelected();
                    phase = Phase.IDLE;
                    return true;
                }

                minDelayMs = (tapSpeed >= 95) ? script.random(14, 20) : script.random(18, 28);
                nextAtMs = System.currentTimeMillis() + minDelayMs;
                phase = Phase.WAIT_MIN;

                script.submitTask(() -> false, minDelayMs);
                return true;

            case WAIT_MIN:
                phase = Phase.SECOND_CLICK;
                return execute();

            case SECOND_CLICK:
                inv = script.getWidgetManager().getInventory()
                        .search(Set.of(primaryIngredientID, selectedDartTipID));

                if (inv == null) {
                    phase = Phase.IDLE;
                    return true;
                }

                final int secondId = firstIsPrimary ? selectedDartTipID : primaryIngredientID;

                if (clickInventoryItem(inv, secondId, true) ||
                        clickInventoryItem(inv, secondId, false)) {

                    dartsMade += 10;
                    pairsDone++;

                    firstIsPrimary = !firstIsPrimary;

                    int cooldownMs = calculateHumanCooldown();
                    nextAtMs = System.currentTimeMillis() + cooldownMs;
                    phase = Phase.COOLDOWN;

                    script.submitTask(() -> false, cooldownMs);
                    return true;
                } else {
                    script.getWidgetManager().getInventory().unSelectItemIfSelected();
                    phase = Phase.IDLE;
                    nextAtMs = System.currentTimeMillis();
                    return true;
                }

            case COOLDOWN:
                if (System.currentTimeMillis() >= nextAtMs) {
                    phase = Phase.IDLE;
                }
                return true;
        }

        return true;
    }

    private boolean clickInventoryItem(ItemGroupResult inv, int itemId, boolean bypassHumanDelay) {
        try {
            var item = inv.getRandomItem(itemId);
            if (item == null) {
                return false;
            }

            if (bypassHumanDelay && interactBypass != null) {
                Object result = interactBypass.invoke(item, true);
                if (result instanceof Boolean && (Boolean) result) {
                    return true;
                }
            }

            return item.interact();
        } catch (Throwable e) {
            script.log("DEBUG", "Click error: " + e.getMessage());
            return false;
        }
    }

    private int calculateHumanCooldown() {
        if (script.random(0, 49) == 0) {
            return script.random(1, 3);
        }

        if (pairsDone % script.random(35, 70) == 0) {
            return script.random(10, 22);
        }

        if (pairsDone % script.random(280, 480) == 0) {
            return script.random(50, 120);
        }

        return script.random(0, 2);
    }
}
