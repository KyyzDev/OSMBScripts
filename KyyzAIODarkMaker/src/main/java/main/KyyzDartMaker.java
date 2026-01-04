package main;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import tasks.FletchTask;
import tasks.Setup;
import utils.Task;
import utils.StatsReporter;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ScriptDefinition(
        name = "Kyyz Dart Maker",
        description = "Fletch darts using proper OSMB dialogue handling. Supports all dart types.",
        skillCategory = SkillCategory.FLETCHING,
        version = 1.1,
        author = "Kyyz"
)
public class KyyzDartMaker extends Script {
    public static final String scriptVersion = "1.1";
    public static boolean setupDone = false;
    public static String task = "Initialize";
    public static int dartsMade = 0;
    public static long startTime = System.currentTimeMillis();

    public static int selectedDartTipID = 822;
    public static int primaryIngredientID = 314;
    public static int resultingDartID = 810;
    public static double xpPerDart = 11.2;

    public static final int ATLATL_DART_TIP = 30998;
    public static final int HEADLESS_ATLATL_DART = 30996;

    private List<Task> tasks;
    private StatsReporter statsReporter;

    private static final Font FONT_TITLE = new Font("Small Fonts", Font.BOLD, 14);
    private static final Font FONT_LABEL = new Font("Small Fonts", Font.PLAIN, 10);
    private static final Font FONT_VALUE = new Font("Small Fonts", Font.BOLD, 10);

    public KyyzDartMaker(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        setupDone = false;
        dartsMade = 0;
        task = "Initialize";
        startTime = System.currentTimeMillis();

        ScriptUI ui = new ScriptUI(this);
        javafx.scene.Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Kyyz Dart Maker", true);

        selectedDartTipID = ui.getSelectedDartTipID();
        configureDartType(selectedDartTipID);

        tasks = Arrays.asList(
                new Setup(this),
                new FletchTask(this)
        );

        statsReporter = new StatsReporter("dart-maker");
        statsReporter.start(() -> {
            int totalXp = (int) Math.round(dartsMade * xpPerDart);
            return Map.of(
                "dartsMade", dartsMade,
                "xpGained", totalXp,
                "runtime", (System.currentTimeMillis() - startTime) / 60000
            );
        });
    }

    public void onStop() {
        if (statsReporter != null) {
            statsReporter.stop();
        }
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

        final Color osrsBrown = new Color(62, 53, 41, 240);
        final Color osrsYellow = new Color(255, 255, 0);
        final Color osrsOrange = new Color(255, 152, 31);
        final Color osrsGreen = new Color(0, 255, 0);
        final Color osrsWhite = new Color(255, 255, 255);
        final Color osrsBorder = new Color(0, 0, 0);

        final int labelColor = osrsWhite.getRGB();
        final int valueYellow = osrsYellow.getRGB();
        final int valueGreen = osrsGreen.getRGB();

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        int totalLines = 6;
        int innerHeight = titleHeight + (totalLines * lineGap) + topGap + 8;

        c.fillRect(innerX - 2, innerY - 2, innerWidth + 4, innerHeight + 4, osrsBorder.getRGB(), 1);

        c.fillRect(innerX, innerY, innerWidth, innerHeight, osrsBrown.getRGB(), 1);

        c.fillRect(innerX, innerY, innerWidth, titleHeight, new Color(52, 43, 31, 240).getRGB(), 1);

        String title = "Kyyz Dart Maker";
        int titleX = innerX + (innerWidth / 2) - (c.getFontMetrics(FONT_TITLE).stringWidth(title) / 2);
        int titleY = innerY + 26;
        c.drawText(title, titleX, titleY, valueYellow, FONT_TITLE);

        int sepY = innerY + titleHeight;
        c.fillRect(innerX, sepY, innerWidth, 1, osrsBorder.getRGB(), 1);

        int curY = innerY + titleHeight + topGap + lineGap;

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
        c.drawText(label, innerX + paddingX, y, labelColor, FONT_LABEL);

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
        if (tipID == ATLATL_DART_TIP) {
            primaryIngredientID = HEADLESS_ATLATL_DART;
        } else {
            primaryIngredientID = 314;
        }

        switch (tipID) {
            case 819:
                resultingDartID = 806;
                xpPerDart = 1.8;
                break;
            case 820:
                resultingDartID = 807;
                xpPerDart = 3.8;
                break;
            case 821:
                resultingDartID = 808;
                xpPerDart = 7.5;
                break;
            case 822:
                resultingDartID = 810;
                xpPerDart = 11.2;
                break;
            case 823:
                resultingDartID = 811;
                xpPerDart = 15.0;
                break;
            case 824:
                resultingDartID = 812;
                xpPerDart = 18.8;
                break;
            case 11232:
                resultingDartID = 11230;
                xpPerDart = 25.0;
                break;
            case 25853:
                resultingDartID = 25849;
                xpPerDart = 21.0;
                break;
            case 30998:
                try {
                    resultingDartID = (int) com.osmb.api.item.ItemID.class.getField("ATLATL_DART").get(null);
                } catch (Exception e) {
                    resultingDartID = 810;
                }
                xpPerDart = 25.0;
                break;
            default:
                resultingDartID = 810;
                xpPerDart = 11.2;
        }
    }
}
