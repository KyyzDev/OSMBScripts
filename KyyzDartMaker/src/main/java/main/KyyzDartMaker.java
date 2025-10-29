package main;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import tasks.FletchTask;
import tasks.Setup;
import utils.Task;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@ScriptDefinition(
        name = "Kyyz Dart Maker",
        description = "Fletch darts efficiently with proper async handling. Supports all dart types.",
        skillCategory = SkillCategory.FLETCHING,
        version = 1.0,
        author = "Kyyz"
)
public class KyyzDartMaker extends Script {
    public static final String scriptVersion = "1.0";
    public static boolean setupDone = false;
    public static String task = "Initialize";
    public static int dartsMade = 0;
    public static long startTime = System.currentTimeMillis();

    // Dart tip configurations
    public static int selectedDartTipID = 822; // Default: Mithril
    public static int primaryIngredientID = 314; // Feather
    public static int resultingDartID = 810; // Mithril dart
    public static double xpPerDart = 11.2; // Mithril XP

    // Atlatl dart support
    public static final int ATLATL_DART_TIP = 30998;
    public static final int HEADLESS_ATLATL_DART = 30996;

    // Speed setting
    public static int tapSpeed = 50; // 1-100 (higher = faster)

    private List<Task> tasks;

    // OSRS pixelated fonts - using Small Fonts for authentic RuneScape look
    private static final Font FONT_TITLE = new Font("Small Fonts", Font.BOLD, 14);
    private static final Font FONT_LABEL = new Font("Small Fonts", Font.PLAIN, 10);
    private static final Font FONT_VALUE = new Font("Small Fonts", Font.BOLD, 10);

    public KyyzDartMaker(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        log("INFO", "Starting Kyyz Dart Maker v" + scriptVersion);

        // Show GUI and WAIT for user to click Start
        ScriptUI ui = new ScriptUI(this);
        javafx.scene.Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Kyyz Dart Maker", true);  // TRUE = wait for user to close dialog

        // Get user selections AFTER they clicked Start
        selectedDartTipID = ui.getSelectedDartTipID();
        tapSpeed = ui.getTapSpeed();

        // Configure dart type based on selection
        configureDartType(selectedDartTipID);

        log("INFO", "===========================================");
        log("INFO", "  Dart Type: " + getItemManager().getItemName(resultingDartID));
        log("INFO", "  Tip: " + getItemManager().getItemName(selectedDartTipID));
        log("INFO", "  Primary: " + getItemManager().getItemName(primaryIngredientID));
        log("INFO", "  XP/dart: " + xpPerDart);
        log("INFO", "  Speed: " + tapSpeed + "%");
        log("INFO", "===========================================");

        tasks = Arrays.asList(
                new Setup(this),
                new FletchTask(this)
        );

        log("INFO", "Script initialized!");
    }

    @Override
    public int poll() {
        if (tasks != null) {
            for (Task task : tasks) {
                if (task.activate()) {
                    task.execute();
                    return 0;
                }
            }
        }
        return 0;
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        String runtime = formatRuntime(elapsed);

        double hours = Math.max(1e-9, elapsed / 3_600_000.0);
        int dartsPerHour = (int) Math.round(dartsMade / hours);
        int totalXp = (int) Math.round(dartsMade * xpPerDart);
        int xpPerHour = (int) Math.round(totalXp / hours);

        final int x = 10;
        final int baseY = 50;
        final int width = 240;
        final int paddingX = 12;
        final int topGap = 8;
        final int lineGap = 18;
        final int titleHeight = 40;

        // OSRS THEME - Classic RuneScape colors
        final Color osrsBrown = new Color(62, 53, 41, 240);      // OSRS brown background
        final Color osrsYellow = new Color(255, 255, 0);         // Classic yellow text
        final Color osrsOrange = new Color(255, 152, 31);        // OSRS orange
        final Color osrsGreen = new Color(0, 255, 0);            // Bright green
        final Color osrsWhite = new Color(255, 255, 255);        // Pure white
        final Color osrsBorder = new Color(0, 0, 0);             // Black border

        final int labelColor = osrsWhite.getRGB();
        final int valueYellow = osrsYellow.getRGB();
        final int valueGreen = osrsGreen.getRGB();

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        int totalLines = 6;  // Runtime, Darts made, Darts/hr, XP gained, XP/hr, Task
        int innerHeight = titleHeight + (totalLines * lineGap) + topGap + 8;

        // OSRS-style simple box
        // Black border (2px)
        c.fillRect(innerX - 2, innerY - 2, innerWidth + 4, innerHeight + 4, osrsBorder.getRGB(), 1);

        // Brown background
        c.fillRect(innerX, innerY, innerWidth, innerHeight, osrsBrown.getRGB(), 1);

        // Title bar (darker brown)
        c.fillRect(innerX, innerY, innerWidth, titleHeight, new Color(52, 43, 31, 240).getRGB(), 1);

        // Title
        String title = "Kyyz Dart Maker";
        int titleX = innerX + (innerWidth / 2) - (c.getFontMetrics(FONT_TITLE).stringWidth(title) / 2);
        int titleY = innerY + 26;
        c.drawText(title, titleX, titleY, valueYellow, FONT_TITLE);

        // Separator line
        int sepY = innerY + titleHeight;
        c.fillRect(innerX, sepY, innerWidth, 1, osrsBorder.getRGB(), 1);

        int curY = innerY + titleHeight + topGap + lineGap;

        // Draw stats - OSRS style (simple and clean)
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Runtime", runtime, labelColor, labelColor);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Darts made", String.valueOf(dartsMade), labelColor, valueGreen);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Darts/hr", String.valueOf(dartsPerHour), labelColor, valueYellow);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "XP gained", formatNumber(totalXp), labelColor, valueGreen);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "XP/hr", formatNumber(xpPerHour), labelColor, valueYellow);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Task", task, labelColor, labelColor);
    }

    private void drawOSRSStatLine(Canvas c, int innerX, int innerWidth, int paddingX, int y,
                                  String label, String value, int labelColor, int valueColor) {
        // Simple OSRS-style: Label on left, value on right
        c.drawText(label, innerX + paddingX, y, labelColor, FONT_LABEL);

        // Value right-aligned
        int valW = c.getFontMetrics(FONT_LABEL).stringWidth(value);
        int valX = innerX + innerWidth - paddingX - valW;
        c.drawText(value, valX, y, valueColor, FONT_LABEL);
    }

    private String formatRuntime(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private String formatNumber(int number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    private void configureDartType(int tipID) {
        // Set primary ingredient (feather or headless atlatl dart)
        if (tipID == ATLATL_DART_TIP) {
            primaryIngredientID = HEADLESS_ATLATL_DART;
        } else {
            primaryIngredientID = 314; // Feather
        }

        // Map tip to dart result
        switch (tipID) {
            case 819:  // Bronze
                resultingDartID = 806;
                xpPerDart = 1.8;
                break;
            case 820:  // Iron
                resultingDartID = 807;
                xpPerDart = 3.8;
                break;
            case 821:  // Steel
                resultingDartID = 808;
                xpPerDart = 7.5;
                break;
            case 822:  // Mithril
                resultingDartID = 810;
                xpPerDart = 11.2;
                break;
            case 823:  // Adamant
                resultingDartID = 811;
                xpPerDart = 15.0;
                break;
            case 824:  // Rune
                resultingDartID = 812;
                xpPerDart = 18.8;
                break;
            case 11232: // Dragon
                resultingDartID = 11230;
                xpPerDart = 25.0;
                break;
            case 25853: // Amethyst
                resultingDartID = 25849;
                xpPerDart = 21.0;
                break;
            case 30998: // Atlatl
                try {
                    resultingDartID = (int) com.osmb.api.item.ItemID.class.getField("ATLATL_DART").get(null);
                } catch (Exception e) {
                    resultingDartID = 810; // Fallback
                }
                xpPerDart = 25.0;
                break;
            default:
                resultingDartID = 810;  // Default Mithril
                xpPerDart = 11.2;
        }
    }
}
