package main;

import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import tasks.ProcessTask;
import tasks.Setup;
import utils.Task;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;

@ScriptDefinition(
        name = "DartMaker",
        description = "Attach feathers to dart tips. Lightweight, no banking. Supports all dart tips.",
        skillCategory = SkillCategory.FLETCHING,
        version = 1.1,
        author = "Kyyz"
)
public class MithrilDartFletcher extends Script {

    // ===== Shared state (used by tasks) =====
    public static boolean setupDone   = false;
    public static boolean hasReqs     = true;
    public static boolean shouldBank  = false; // no banking

    public static int FeatherID = ItemID.FEATHER;

    // Selected tip from UI (default: Mithril tip)
    public static int SelectedDartTipID = ItemID.MITHRIL_DART_TIP;

    // Resulting dart ID (mapped from tip; default: Mithril dart)
    public static int dartID = ItemID.MITHRIL_DART;

    // Stats / paint
    public static int craftCount = 0;       // total darts (not actions)
    public static String task    = "Initialize";
    public static long startTime = System.currentTimeMillis();

    // Click rate scalar 1..100 (UI returns 100 in your current ScriptUI)
    public static int tapSpeed = 50;

    private List<Task> tasks;
    private static final Font ARIEL = Font.getFont("Arial");

    public MithrilDartFletcher(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        log("INFO", "Starting DartMaker");

        // Optional UI: select tip + confirm; falls back to defaults if UI fails
        try {
            ScriptUI ui = new ScriptUI(this);
            Scene scene = ui.buildScene(this);
            getStageController().show(scene, "Dart Options", false);

            // Save chosen tip + speed
            int selectedTip = ui.getSelectedDartID();
            SelectedDartTipID = selectedTip;
            dartID = mapTipToDart(selectedTip);

            try {
                tapSpeed = ui.getTapSpeed(); // your current UI returns 100
            } catch (Throwable ignored) {
                // keep default 50
            }

            log("INFO", "Making: " + getItemManager().getItemName(SelectedDartTipID) +
                    "  → Result: " + safeName(dartID) +
                    "  | tapSpeed=" + tapSpeed);

        } catch (Throwable t) {
            log("INFO", "UI not shown; using defaults (Mithril Tip, tapSpeed=" + tapSpeed + ")");
        }

        tasks = Arrays.asList(
                new Setup(this),
                new ProcessTask(this) // uses SelectedDartTipID + FeatherID + tapSpeed
        );
    }

    @Override
    public int poll() {
        for (Task t : tasks) {
            if (t.activate()) {
                t.execute();
                return 0;
            }
        }
        return 0;
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
        int dartsPerHour = (int)((craftCount * 3_600_000L) / elapsed);

        // Keeping your existing XP model:
        // 112 XP per 10 darts (one action)
        int actions = craftCount / 10;
        int totalXp = actions * 112;
        int xpPerHour = (int)((totalXp * 3_600_000L) / elapsed);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        int y = 40;
        c.fillRect(5, y, 220, 130, Color.BLACK.getRGB(), 0.75f);
        c.drawRect(5, y, 220, 130, Color.BLACK.getRGB());
        c.drawText("Darts made: " + f.format(craftCount), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Darts/hr: " + f.format(dartsPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP gained: " + f.format(totalXp), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP/hr: " + f.format(xpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Task: " + task, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
    }

    // ===== Helpers =====

    private int mapTipToDart(int tipId) {
        if (tipId == ItemID.BRONZE_DART_TIP)   return ItemID.BRONZE_DART;
        if (tipId == ItemID.IRON_DART_TIP)     return ItemID.IRON_DART;
        if (tipId == ItemID.STEEL_DART_TIP)    return ItemID.STEEL_DART;
        if (tipId == ItemID.MITHRIL_DART_TIP)  return ItemID.MITHRIL_DART;
        if (tipId == ItemID.ADAMANT_DART_TIP)  return ItemID.ADAMANT_DART;
        if (tipId == ItemID.RUNE_DART_TIP)     return ItemID.RUNE_DART;
        if (tipId == ItemID.DRAGON_DART_TIP)   return ItemID.DRAGON_DART;
        if (tipId == ItemID.AMETHYST_DART_TIP) return ItemID.AMETHYST_DART;
        // Atlatl darts — if your API has a result item, map it; otherwise leave Mithril as safe default
        try {
            // Replace with the correct constant if available:
            return (int) ItemID.class.getField("ATLATL_DART").get(null);
        } catch (Throwable ignored) {
            return ItemID.MITHRIL_DART;
        }
    }

    private String safeName(int itemId) {
        try { return getItemManager().getItemName(itemId); }
        catch (Throwable t) { return String.valueOf(itemId); }
    }
}
