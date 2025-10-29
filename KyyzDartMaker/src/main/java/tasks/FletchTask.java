package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;
import utils.Task;

import java.lang.reflect.Method;
import java.util.Set;

import static main.KyyzDartMaker.*;

/**
 * Fletches darts using PROPER ASYNC PATTERNS.
 * Uses submitTask() instead of script.sleep() to avoid locking the executor.
 * This allows proper pausing/stopping and keeps the UI responsive.
 */
public class FletchTask extends Task {

    private enum Phase {
        IDLE,           // Waiting to start next pair
        FIRST_CLICK,    // Click primary ingredient (feather/headless)
        WAIT_MIN,       // Minimum delay before second click
        SECOND_CLICK,   // Click dart tip
        COOLDOWN        // Brief pause between pairs
    }

    private Phase phase = Phase.IDLE;
    private boolean firstIsPrimary = true;
    private long nextAtMs = 0;
    private int minDelayMs = 20;

    // Bypass method for faster clicking (uses internal API if available)
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

        // Check if we have both ingredients
        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(primaryIngredientID, selectedDartTipID));

        if (inv == null) {
            return false;
        }

        // Activate if we have both items, OR if we're in the middle of a fletching sequence
        boolean hasSupplies = inv.contains(primaryIngredientID) && inv.contains(selectedDartTipID);
        boolean inProgress = phase != Phase.IDLE;

        return hasSupplies || inProgress;
    }

    @Override
    public boolean execute() {
        task = "Fletching darts";

        // Check if we need to wait before proceeding
        if (System.currentTimeMillis() < nextAtMs) {
            // Use submitTask to wait without blocking!
            long waitTime = nextAtMs - System.currentTimeMillis();
            script.submitTask(() -> false, (int) Math.max(1, waitTime));
            return true;
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(primaryIngredientID, selectedDartTipID));

        if (inv == null) {
            return true;
        }

        // One-time check for bypass method
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
                // Start new fletching pair
                phase = Phase.FIRST_CLICK;
                return execute(); // Immediately continue to first click

            case FIRST_CLICK:
                final int firstId = firstIsPrimary ? primaryIngredientID : selectedDartTipID;

                // Try to click first item (with bypass if available, fallback to normal)
                if (!clickInventoryItem(inv, firstId, true) &&
                        !clickInventoryItem(inv, firstId, false)) {
                    script.getWidgetManager().getInventory().unSelectItemIfSelected();
                    phase = Phase.IDLE;
                    return true;
                }

                // Calculate delay based on tap speed
                minDelayMs = (tapSpeed >= 95) ? script.random(14, 20) : script.random(18, 28);
                nextAtMs = System.currentTimeMillis() + minDelayMs;
                phase = Phase.WAIT_MIN;

                // Use submitTask to wait asynchronously!
                script.submitTask(() -> false, minDelayMs);
                return true;

            case WAIT_MIN:
                // Move to second click phase
                phase = Phase.SECOND_CLICK;
                return execute(); // Immediately continue to second click

            case SECOND_CLICK:
                // Refresh inventory for second click
                inv = script.getWidgetManager().getInventory()
                        .search(Set.of(primaryIngredientID, selectedDartTipID));

                if (inv == null) {
                    phase = Phase.IDLE;
                    return true;
                }

                final int secondId = firstIsPrimary ? selectedDartTipID : primaryIngredientID;

                // Try to click second item
                if (clickInventoryItem(inv, secondId, true) ||
                        clickInventoryItem(inv, secondId, false)) {

                    // Successfully made 10 darts!
                    dartsMade += 10;
                    pairsDone++;

                    // Alternate which item we click first (more human-like)
                    firstIsPrimary = !firstIsPrimary;

                    // Humanized delays
                    int cooldownMs = calculateHumanCooldown();
                    nextAtMs = System.currentTimeMillis() + cooldownMs;
                    phase = Phase.COOLDOWN;

                    // Use submitTask for cooldown!
                    script.submitTask(() -> false, cooldownMs);
                    return true;
                } else {
                    // Failed to click second item
                    script.getWidgetManager().getInventory().unSelectItemIfSelected();
                    phase = Phase.IDLE;
                    nextAtMs = System.currentTimeMillis();
                    return true;
                }

            case COOLDOWN:
                // Check if cooldown is done
                if (System.currentTimeMillis() >= nextAtMs) {
                    phase = Phase.IDLE;
                }
                return true;
        }

        return true;
    }

    /**
     * Clicks an inventory item using bypass method if available, otherwise normal interaction
     */
    private boolean clickInventoryItem(ItemGroupResult inv, int itemId, boolean bypassHumanDelay) {
        try {
            var item = inv.getRandomItem(itemId);
            if (item == null) {
                return false;
            }

            // Try bypass method first (faster, no extra delays)
            if (bypassHumanDelay && interactBypass != null) {
                Object result = interactBypass.invoke(item, true);
                if (result instanceof Boolean && (Boolean) result) {
                    return true;
                }
            }

            // Fallback to normal interaction
            return item.interact();
        } catch (Throwable e) {
            script.log("DEBUG", "Click error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calculate humanized cooldown between dart pairs
     */
    private int calculateHumanCooldown() {
        // Occasional very short delays (2% chance)
        if (script.random(0, 49) == 0) {
            return script.random(1, 3);
        }

        // Small pause every ~50 pairs (to simulate fatigue/attention)
        if (pairsDone % script.random(35, 70) == 0) {
            return script.random(10, 22);
        }

        // Longer break every ~380 pairs (to simulate micro-breaks)
        if (pairsDone % script.random(280, 480) == 0) {
            return script.random(50, 120);
        }

        // Most of the time: minimal delay
        return script.random(0, 2);
    }
}
