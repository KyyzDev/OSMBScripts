package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;
import utils.Task;

import java.lang.reflect.Method;
import java.util.Set;

import static main.MithrilDartFletcher.*;

public class ProcessTask extends Task {

    private enum Phase { FIRST_CLICK, WAIT_MIN, SECOND_CLICK, COOLDOWN }
    private Phase phase = Phase.FIRST_CLICK;

    private boolean firstIsPrimary = true;

    private long nextAtMs = 0;     
    private int  minDelayMs = 20;  

    private static volatile boolean bypassChecked = false;
    private static volatile Method  interactBypass = null;

    private long pairsDone = 0;

    public ProcessTask(Script script) { super(script); }

    @Override
    public boolean activate() {
        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(PrimaryIngredientID, SelectedDartTipID));
        boolean hasSupplies = inv != null && inv.contains(PrimaryIngredientID) && inv.contains(SelectedDartTipID);
        return hasSupplies || phase != Phase.FIRST_CLICK;
    }

    @Override
    public boolean execute() {
        task = "Fletching Darts (non-blocking)";

        if (System.currentTimeMillis() < nextAtMs) return true;

        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(PrimaryIngredientID, SelectedDartTipID));
        if (inv == null) return true;

        if (!bypassChecked) {
            try {
                var probe = inv.getRandomItem(PrimaryIngredientID);
                if (probe != null) interactBypass = probe.getClass().getMethod("interact", boolean.class);
            } catch (Throwable ignored) {}
            bypassChecked = true;
        }

        switch (phase) {
            case FIRST_CLICK -> {
                final int firstId  = firstIsPrimary ? PrimaryIngredientID : SelectedDartTipID;

                if (!clickInventoryItem(inv, firstId, /*bypass*/true) &&
                        !clickInventoryItem(inv, firstId, /*bypass*/false)) {
                    script.getWidgetManager().getInventory().unSelectItemIfSelected();
                    return true;
                }

                minDelayMs = (tapSpeed >= 95) ? script.random(14, 20) : script.random(18, 28);
                nextAtMs = System.currentTimeMillis() + minDelayMs;
                phase = Phase.WAIT_MIN;
                return true;
            }

            case WAIT_MIN -> {
                phase = Phase.SECOND_CLICK;
                return true;
            }

            case SECOND_CLICK -> {
                inv = script.getWidgetManager().getInventory()
                        .search(Set.of(PrimaryIngredientID, SelectedDartTipID));
                if (inv == null) return true;

                final int secondId = firstIsPrimary ? SelectedDartTipID : PrimaryIngredientID;

                if (clickInventoryItem(inv, secondId, /*bypass*/true) ||
                        clickInventoryItem(inv, secondId, /*bypass*/false)) {
                    craftCount += 10;
                    pairsDone++;
                    phase = Phase.COOLDOWN;

                    if (script.random(0, 49) == 0)                     nextAtMs = System.currentTimeMillis() + script.random(1, 3);
                    else if (pairsDone % script.random(35, 70) == 0)   nextAtMs = System.currentTimeMillis() + script.random(10, 22);
                    else if (pairsDone % script.random(280, 480) == 0) nextAtMs = System.currentTimeMillis() + script.random(50, 120);
                    else                                                nextAtMs = System.currentTimeMillis();

                    firstIsPrimary = !firstIsPrimary;
                    return true;
                }

                script.getWidgetManager().getInventory().unSelectItemIfSelected();
                phase = Phase.FIRST_CLICK;
                nextAtMs = System.currentTimeMillis();
                return true;
            }

            case COOLDOWN -> {
                if (System.currentTimeMillis() >= nextAtMs) {
                    phase = Phase.FIRST_CLICK;
                }
                return true;
            }
        }

        return true;
    }

    private boolean clickInventoryItem(ItemGroupResult inv, int itemId, boolean bypassHumanDelay) {
        try {
            var item = inv.getRandomItem(itemId);
            if (item == null) return false;

            if (bypassHumanDelay && interactBypass != null) {
                Object ok = interactBypass.invoke(item, true);
                if (ok instanceof Boolean && (Boolean) ok) return true;
            }
            return item.interact();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
