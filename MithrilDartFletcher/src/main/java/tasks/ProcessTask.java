package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import utils.Task;

import java.lang.reflect.Method;
import java.util.Set;

import static main.MithrilDartFletcher.*;

public class ProcessTask extends Task {

    // Pattern: 1 2 2 1   (Feather, Tip, Tip, Feather)
    private static final int[] SEQUENCE = new int[] {
            FeatherID, ItemID.MITHRIL_DART_TIP, ItemID.MITHRIL_DART_TIP, FeatherID
    };

    // Track completions so we can sprinkle rare pauses
    private long pairCompletionsTotal = 0;

    // Cache interact(boolean) if the client supports it (avoid reflection per click)
    private static volatile boolean bypassChecked = false;
    private static volatile Method interactBypass = null;

    public ProcessTask(Script script) { super(script); }

    @Override
    public boolean activate() {
        // Run whenever both items exist in inventory
        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(FeatherID, ItemID.MITHRIL_DART_TIP));
        return inv != null && inv.contains(FeatherID) && inv.contains(ItemID.MITHRIL_DART_TIP);
    }

    @Override
    public boolean execute() {
        task = "Fleching Darts";

        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(FeatherID, ItemID.MITHRIL_DART_TIP));
        if (inv == null || !inv.contains(FeatherID) || !inv.contains(ItemID.MITHRIL_DART_TIP)) return false;

        // detect/cached interact(boolean) once
        if (!bypassChecked) {
            try {
                var sample = inv.getRandomItem(FeatherID);
                if (sample != null) {
                    interactBypass = sample.getClass().getMethod("interact", boolean.class);
                }
            } catch (Throwable ignored) {}
            bypassChecked = true;
        }

        // Slider → how many full sequences per poll (keep your feel)
        final int sequencesThisTick = Math.max(5, tapSpeed * 40);

        int lastClicked = -1;

        for (int n = 0; n < sequencesThisTick; n++) {
            for (int id : SEQUENCE) {
                // we drive pairs explicitly via combinePair below
                final int firstId  = (lastClicked == FeatherID || id == FeatherID) ? FeatherID : ItemID.MITHRIL_DART_TIP;
                final int secondId = (firstId == FeatherID) ? ItemID.MITHRIL_DART_TIP : FeatherID;

                if (combinePair(inv, firstId, secondId)) {
                    craftCount += 10;      // 10 darts per completed pair
                    pairCompletionsTotal++;

                    // ====== MICRO JITTERS (anti-ban) ======
                    // keep them tiny & infrequent, and ONLY after a completed pair
                    if (script.random(0, 49) == 0) {
                        script.sleep(script.random(1, 3));     // ~2%: 1–3ms
                    }
                    if (pairCompletionsTotal % script.random(35, 70) == 0) {
                        script.sleep(script.random(10, 22));   // small breath
                    }
                    if (pairCompletionsTotal % script.random(280, 480) == 0) {
                        script.sleep(script.random(50, 120));  // rare hesitation
                    }
                } else {
                    // selection recovery if pair failed
                    script.getWidgetManager().getInventory().unSelectItemIfSelected();
                }

                lastClicked = id;
            }
        }

        return true;
    }

    /**
     * Click first -> enforce a tiny minimum delay -> refresh -> click second.
     * If the second click fails, unselect and retry the pair once (with the same safe delay).
     *
     * The minimum delay is tiny (approx 18–28ms; 14–20ms when tapSpeed>=95) so we avoid
     * server-side cancel while still feeling instant.
     */
    private boolean combinePair(ItemGroupResult inv, int firstId, int secondId) {
        // FIRST CLICK
        if (!clickInventoryItem(inv, firstId, true) && !clickInventoryItem(inv, firstId, false)) {
            return false;
        }

        // safe minimal per-pair delay (prevents cancel/no-XP)
        final int minDelay = (tapSpeed >= 95) ? script.random(14, 20) : script.random(18, 28);
        script.sleep(minDelay);

        // refresh snapshot to avoid stale refs
        inv = script.getWidgetManager().getInventory()
                .search(Set.of(FeatherID, ItemID.MITHRIL_DART_TIP));
        if (inv == null) return false;

        // SECOND CLICK
        if (clickInventoryItem(inv, secondId, true) || clickInventoryItem(inv, secondId, false)) {
            return true;
        }

        // ====== RECOVERY (kept intact) ======
        // unselect, very tiny settle, then retry once with the same safe delay between clicks
        script.getWidgetManager().getInventory().unSelectItemIfSelected();
        script.sleep(script.random(4, 8));

        inv = script.getWidgetManager().getInventory()
                .search(Set.of(FeatherID, ItemID.MITHRIL_DART_TIP));
        if (inv == null) return false;

        if (!clickInventoryItem(inv, firstId, true) && !clickInventoryItem(inv, firstId, false)) {
            return false;
        }

        script.sleep(minDelay);

        inv = script.getWidgetManager().getInventory()
                .search(Set.of(FeatherID, ItemID.MITHRIL_DART_TIP));
        if (inv == null) return false;

        return clickInventoryItem(inv, secondId, true) || clickInventoryItem(inv, secondId, false);
    }

    /**
     * Clicks an inventory item, attempting to bypass the humanised delay if supported.
     * Falls back to normal interact() if no such overload exists.
     */
    private boolean clickInventoryItem(ItemGroupResult inv, int itemId, boolean bypassHumanDelay) {
        try {
            var item = inv.getRandomItem(itemId);
            if (item == null) return false;

            if (bypassHumanDelay && interactBypass != null) {
                Object ok = interactBypass.invoke(item, true);
                if (ok instanceof Boolean && (Boolean) ok) return true;
                // fall through if API returns void or non-boolean
            }

            return item.interact();

        } catch (Throwable t) {
            return false;
        }
    }
}
