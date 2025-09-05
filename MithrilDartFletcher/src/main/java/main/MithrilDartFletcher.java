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
        description = "Attach feathers/headless darts to tips. Lightweight, no banking. Supports all dart tips.",
        skillCategory = SkillCategory.FLETCHING,
        version = 1.3,
        author = "Kyyz"
)
public class MithrilDartFletcher extends Script {

    // ===== Custom IDs not present in ItemID (adjust if your API differs) =====
    public static final int ATLATL_DART_TIP = 30998;

    // ===== XP per dart (from OSRS wiki table). Atlatl is overrideable (set to your real value). =====
    private static final double XP_BRONZE     = 1.8;
    private static final double XP_IRON       = 3.8;
    private static final double XP_STEEL      = 7.5;
    private static final double XP_MITHRIL    = 11.2;
    private static final double XP_ADAMANT    = 15.0;
    private static final double XP_RUNE       = 18.8;
    private static final double XP_AMETHYST   = 21.0;
    private static final double XP_DRAGON     = 25.0;
    private static final double XP_ATLATL     = 25.0; // <-- UPDATE if your server defines a different value

    // ===== Shared state (used by tasks) =====
    public static boolean setupDone   = false;
    public static boolean hasReqs     = true;
    public static boolean shouldBank  = false; // no banking

    public static int FeatherID        = ItemID.FEATHER;
    public static int HeadlessAtlatlID = ItemID.HEADLESS_ATLATL_DART;

    // which primary ingredient to pair with the selected tip:
    //  - normal darts: Feather
    //  - atlatl darts: Headless Atlatl
    public static int PrimaryIngredientID = FeatherID;

    // Selected tip from UI (default: Mithril tip)
    public static int SelectedDartTipID = ItemID.MITHRIL_DART_TIP;

    // Result dart ID (mapped from tip; default Mithril)
    public static int dartID = ItemID.MITHRIL_DART;

    // Stats / paint
    public static int craftCount = 0;       // total darts (not actions)
    public static String task    = "Initialize";
    public static long startTime = System.currentTimeMillis();

    // Click rate scalar 1..100 (UI returns 100 in your ScriptUI)
    public static int tapSpeed = 50;

    private List<Task> tasks;
    private static final Font ARIEL = Font.getFont("Arial");

    // Header colors (OSRS-ish)
    private static final int HEADER_DARK     = new Color(36, 39, 43).getRGB();
    private static final int HEADER_BORDER   = new Color(12, 13, 14).getRGB();
    private static final int HEADER_GOLD     = new Color(216, 171, 58).getRGB();
    private static final int HEADER_GOLD_DK  = new Color(152, 115, 30).getRGB();

    public MithrilDartFletcher(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        log("INFO", "Starting AIO Dart Maker");

        // UI selection
        try {
            ScriptUI ui = new ScriptUI(this);
            Scene scene = ui.buildScene(this);
            getStageController().show(scene, "Dart Options", false);

            int selectedTip = ui.getSelectedDartID();
            SelectedDartTipID = selectedTip;
            dartID = mapTipToDart(selectedTip);

            // Atlatl uses headless darts instead of feathers
            if (selectedTip == ATLATL_DART_TIP) {
                PrimaryIngredientID = HeadlessAtlatlID;
            } else {
                PrimaryIngredientID = FeatherID;
            }

            try { tapSpeed = ui.getTapSpeed(); } catch (Throwable ignored) {}

            log("INFO", "Tip: " + getItemManager().getItemName(SelectedDartTipID) +
                    " | Ingredient: " + safeName(PrimaryIngredientID) +
                    " | Result: " + safeName(dartID) +
                    " | XP/dart=" + xpPerDartForTip(SelectedDartTipID) +
                    " | tapSpeed=" + tapSpeed);

        } catch (Throwable t) {
            log("INFO", "UI not shown; defaults (Mithril tip, Feathers), tapSpeed=" + tapSpeed);
            SelectedDartTipID = ItemID.MITHRIL_DART_TIP;
            PrimaryIngredientID = FeatherID;
            dartID = ItemID.MITHRIL_DART;
        }

        tasks = Arrays.asList(
                new Setup(this),
                new ProcessTask(this) // uses SelectedDartTipID + PrimaryIngredientID + tapSpeed
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

        // LIVE XP per dart based on selected tip
        double xpPerDart = xpPerDartForTip(SelectedDartTipID);

        // craftCount counts DARTS (we add 10 per pair)
        int totalXp    = (int)Math.round(craftCount * xpPerDart);
        int dartsPerHr = (int)((craftCount * 3_600_000L) / elapsed);
        int xpPerHr    = (int)((totalXp   * 3_600_000L) / elapsed);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        // Panel box
        int panelX = 5;
        int panelY = 40;     // slightly lower
        int panelW = 260;    // a bit wider so header breathes
        int panelH = 170;

        // Draw panel
        c.fillRect(panelX, panelY, panelW, panelH, Color.BLACK.getRGB(), 0.75f);
        c.drawRect(panelX, panelY, panelW, panelH, Color.BLACK.getRGB());

        // -------- Banner INSIDE the panel --------
        int headerPad = 8;          // padding from panel edges
        int headerH   = 32;         // banner height
        int headerX   = panelX + headerPad;
        int headerY   = panelY + headerPad;
        int headerW   = panelW - headerPad * 2;

        // If you’re using the faux-drawn header:
        drawHeader(c, headerX, headerY, headerW, headerH, "AIO Dart Maker");

        // If you switched back to PNG banner + drawImage, call it here instead:
        // drawBanner(c, headerX, headerY, headerW, headerH, 1.0f);

        // -------- Text below the header --------
        int y = headerY + headerH + 14; // gap under header

        c.drawText("Darts made: " + f.format(craftCount), panelX + 10, y, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Darts/hr: "   + f.format(dartsPerHr), panelX + 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP gained: "  + f.format(totalXp),    panelX + 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP/hr: "      + f.format(xpPerHr),    panelX + 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Task: "       + task,                 panelX + 10, y += 20, Color.WHITE.getRGB(), ARIEL);
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

        // Atlatl mapping (ItemID may not define ATLATL_* constants)
        if (tipId == ATLATL_DART_TIP) {
            try {
                // If your API exposes the crafted item id:
                return (int) ItemID.class.getField("ATLATL_DART").get(null);
            } catch (Throwable ignored) {
                // Fallback: use mithril dart if crafted ID is unknown
                return ItemID.MITHRIL_DART;
            }
        }

        // sensible fallback
        return ItemID.MITHRIL_DART;
    }

    private double xpPerDartForTip(int tipId) {
        if (tipId == ItemID.BRONZE_DART_TIP)   return XP_BRONZE;
        if (tipId == ItemID.IRON_DART_TIP)     return XP_IRON;
        if (tipId == ItemID.STEEL_DART_TIP)    return XP_STEEL;
        if (tipId == ItemID.MITHRIL_DART_TIP)  return XP_MITHRIL;
        if (tipId == ItemID.ADAMANT_DART_TIP)  return XP_ADAMANT;
        if (tipId == ItemID.RUNE_DART_TIP)     return XP_RUNE;
        if (tipId == ItemID.DRAGON_DART_TIP)   return XP_DRAGON;
        if (tipId == ItemID.AMETHYST_DART_TIP) return XP_AMETHYST;
        if (tipId == ATLATL_DART_TIP)          return XP_ATLATL; // adjustable
        return XP_MITHRIL; // default safety
    }

    /** Draws a faux "banner" header that looks like the OSRS plaque. */
    private void drawHeader(Canvas c, int x, int y, int w, int h, String title) {
        // dark plate
        c.fillRect(x, y, w, h, HEADER_DARK, 0.95f);
        // black outer border
        c.drawRect(x, y, w, h, HEADER_BORDER);
        // inner gold border (inset 2px)
        c.drawRect(x + 2, y + 2, w - 4, h - 4, HEADER_GOLD_DK);

        // center-ish title (Canvas lacks text metrics; we estimate width)
        int approxCharW = 7; // tweak if it looks off
        int textW = Math.max(title.length() * approxCharW, 0);
        int tx = x + Math.max((w - textW) / 2, 6);
        int ty = y + (h / 2) + 5;

        // subtle "gold" shadow then bright text
        c.drawText(title, tx + 1, ty + 1, HEADER_BORDER, ARIEL);
        c.drawText(title, tx, ty, HEADER_GOLD, ARIEL);
    }

    private String safeName(int itemId) {
        try { return getItemManager().getItemName(itemId); }
        catch (Throwable t) { return String.valueOf(itemId); }
    }
}
