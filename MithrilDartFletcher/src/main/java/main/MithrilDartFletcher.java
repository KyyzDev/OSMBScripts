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
        name = "MithDartMaker",
        description = "Creates Mithril Darts.",
        skillCategory = SkillCategory.FLETCHING,
        version = 1.0,
        author = "Kyyz"
)
public class MithrilDartFletcher extends Script {

    // ===== Shared state =====
    public static boolean setupDone = false;
    public static boolean hasReqs   = true;
    public static boolean shouldBank = false; // no banking

    public static int FeatherID = ItemID.FEATHER;
    public static int dartID    = ItemID.MITHRIL_DART;

    // stats/paint
    public static int craftCount = 0;       // total darts (not actions)
    public static String task    = "Initialize";
    public static long startTime = System.currentTimeMillis();

    // slider value 1..100 (default if UI absent)
    public static int tapSpeed = 50;

    private List<Task> tasks;
    private static final Font ARIEL = Font.getFont("Arial");

    public MithrilDartFletcher(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        log("INFO", "Starting MithDartMaker");

        // Optional UI: we keep it if you still use ScriptUI; otherwise we fall back gracefully
        try {
            ScriptUI ui = new ScriptUI(this);
            Scene scene = ui.buildScene(this);
            getStageController().show(scene, "Dart Options", false);

            int selected = ui.getSelectedDartID(); // keeps your existing UI flow
            log("INFO", "We're making " + getItemManager().getItemName(selected) + " this run.");

            // may not exist on older ScriptUI; swallow if missing
            try {
            } catch (Throwable ignored) {
                // keep default 50
            }
        } catch (Throwable t) {
            log("INFO", "UI not shown; using default tapSpeed=" + tapSpeed);
        }

        tasks = Arrays.asList(
                new Setup(this),
                new ProcessTask(this)
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
}
