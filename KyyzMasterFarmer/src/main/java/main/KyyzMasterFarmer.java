package main;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import tasks.*;
import utils.Task;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@ScriptDefinition(
        name = "Kyyz Master Farmer",
        description = "Pickpockets Master Farmer for herb seeds",
        skillCategory = SkillCategory.THIEVING,
        version = 1.0,
        author = "Kyyz"
)
public class KyyzMasterFarmer extends Script {
    public static final String scriptVersion = "1.0";
    public static boolean setupDone = false;
    public static String task = "Initialize";
    public static int pickpocketCount = 0;
    public static int successfulPickpockets = 0;
    public static int failedPickpockets = 0;
    public static long startTime = System.currentTimeMillis();

    // Seed tracking
    public static int guamSeedCount = 0;        // ID: 5291
    public static int marrentillSeedCount = 0;  // ID: 5292
    public static int tarrominSeedCount = 0;    // ID: 5293
    public static int harralanderSeedCount = 0; // ID: 5294
    public static int ranarrSeedCount = 0;      // ID: 5295
    public static int toadflaxSeedCount = 0;    // ID: 5296
    public static int iritSeedCount = 0;        // ID: 5297
    public static int avantoeSeedCount = 0;     // ID: 5298
    public static int kwuarmSeedCount = 0;      // ID: 5299
    public static int snapdragonSeedCount = 0;  // ID: 5300
    public static int cadantineSeedCount = 0;   // ID: 5301
    public static int lantadymeSeedCount = 0;   // ID: 5302
    public static int dwarfWeedSeedCount = 0;   // ID: 5303
    public static int torstolSeedCount = 0;     // ID: 5304

    public static String targetNPC = "Master Farmer";
    public static boolean eatFood = true;
    public static int hpThreshold = 50;  // Eat when HP drops to this value
    public static String lootMode = "DROP_ALL";
    public static String foodName = "Lobster";
    public static int foodItemId = 379;  // Item ID of the food to withdraw
    public static int foodAmount = 4;  // How many food items to withdraw from bank
    public static boolean needFood = false;  // Flag to trigger banking when out of food
    public static boolean useSeedBox = false;  // Use seed box to store seeds automatically
    public static final int SEED_BOX_ID = 13639;  // Open seed box item ID

    // Fixed location for Master Farmer at Draynor
    public static final com.osmb.api.location.position.types.WorldPosition DRAYNOR_FARMER_LOCATION =
        new com.osmb.api.location.position.types.WorldPosition(3079, 3250, 0);

    private List<Task> tasks;

    // OSRS pixelated fonts - using Small Fonts for authentic RuneScape look
    private static final Font FONT_TITLE = new Font("Small Fonts", Font.BOLD, 14);
    private static final Font FONT_SUBTITLE = new Font("Small Fonts", Font.PLAIN, 9);
    private static final Font FONT_LABEL = new Font("Small Fonts", Font.PLAIN, 10);
    private static final Font FONT_VALUE = new Font("Small Fonts", Font.BOLD, 10);

    public KyyzMasterFarmer(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                12598, // Grand Exchange
                12342, // Lumbridge
                10806, // Ardougne
        };
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;  // Enable OSMB's built-in bank tab selection dialog
    }

    @Override
    public void onStart() {
        log("INFO", "Starting Kyyz Master Farmer v" + scriptVersion);

        // Show GUI and WAIT for user to click Start
        ScriptUI ui = new ScriptUI(this);
        javafx.scene.Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Kyyz Master Farmer", true);  // TRUE = wait for user to close dialog

        // Get user selections AFTER they clicked Start
        targetNPC = "Master Farmer";  // Hardcoded
        ScriptUI.FoodType selectedFood = ui.getFoodType();

        // Check if NO_FOOD was selected
        if (selectedFood.name().equals("NO_FOOD")) {
            eatFood = false;
            foodName = "None";
            foodItemId = -1;
            foodAmount = 0;
        } else {
            eatFood = true;
            foodName = selectedFood.getFoodName();
            foodItemId = selectedFood.getItemId();
            foodAmount = ui.getFoodAmount();
        }

        hpThreshold = ui.getHpThreshold();
        ScriptUI.LootMode selectedLootMode = ui.getLootMode();
        lootMode = selectedLootMode.name();
        useSeedBox = ui.useSeedBox();

        log("INFO", "===========================================");
        log("INFO", "  Target: " + targetNPC);
        log("INFO", "  Food: " + (eatFood ? foodAmount + "x " + foodName + " @ " + hpThreshold + " HP" : "Disabled"));
        log("INFO", "  Loot: " + lootMode);
        log("INFO", "  Seed Box: " + (useSeedBox ? "Enabled" : "Disabled"));
        log("INFO", "===========================================");

        tasks = Arrays.asList(
                new Setup(this),
                new EatTask(this),
                new BankTask(this),
                new WalkTask(this),
                new ThieveTask(this)
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
        int pickpocketsPerHour = (int) Math.round(pickpocketCount / hours);

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
        final Color osrsGray = new Color(190, 190, 190);         // Light gray
        final Color osrsBorder = new Color(0, 0, 0);             // Black border

        final int labelColor = osrsWhite.getRGB();
        final int valueYellow = osrsYellow.getRGB();
        final int valueOrange = osrsOrange.getRGB();
        final int valueGreen = osrsGreen.getRGB();

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        // Count how many seed types we've collected
        int seedTypesCollected = 0;
        if (ranarrSeedCount > 0) seedTypesCollected++;
        if (snapdragonSeedCount > 0) seedTypesCollected++;
        if (torstolSeedCount > 0) seedTypesCollected++;
        if (lantadymeSeedCount > 0) seedTypesCollected++;
        if (cadantineSeedCount > 0) seedTypesCollected++;
        if (kwuarmSeedCount > 0) seedTypesCollected++;
        if (avantoeSeedCount > 0) seedTypesCollected++;
        if (iritSeedCount > 0) seedTypesCollected++;
        if (toadflaxSeedCount > 0) seedTypesCollected++;
        if (harralanderSeedCount > 0) seedTypesCollected++;
        if (tarrominSeedCount > 0) seedTypesCollected++;
        if (marrentillSeedCount > 0) seedTypesCollected++;
        if (guamSeedCount > 0) seedTypesCollected++;
        if (dwarfWeedSeedCount > 0) seedTypesCollected++;

        int totalLines = 6 + seedTypesCollected;  // Runtime, Successful, Failed, Per hour, Task, Version + seeds
        int innerHeight = titleHeight + (totalLines * lineGap) + topGap + 8;

        // OSRS-style simple box
        // Black border (2px)
        c.fillRect(innerX - 2, innerY - 2, innerWidth + 4, innerHeight + 4, osrsBorder.getRGB(), 1);

        // Brown background
        c.fillRect(innerX, innerY, innerWidth, innerHeight, osrsBrown.getRGB(), 1);

        // Title bar (darker brown)
        c.fillRect(innerX, innerY, innerWidth, titleHeight, new Color(52, 43, 31, 240).getRGB(), 1);

        // Title
        String title = "Kyyz Master Farmer";
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
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Successful", String.valueOf(successfulPickpockets), labelColor, valueGreen);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Failed", String.valueOf(failedPickpockets), labelColor, osrsOrange.getRGB());

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Per hour", String.valueOf(pickpocketsPerHour), labelColor, valueYellow);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Task", task, labelColor, labelColor);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Version", scriptVersion, labelColor, labelColor);

        // Draw seeds - simple OSRS style
        if (ranarrSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Ranarr seed", String.valueOf(ranarrSeedCount), labelColor, valueGreen); }
        if (snapdragonSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Snapdragon seed", String.valueOf(snapdragonSeedCount), labelColor, valueGreen); }
        if (torstolSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Torstol seed", String.valueOf(torstolSeedCount), labelColor, valueGreen); }
        if (lantadymeSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Lantadyme seed", String.valueOf(lantadymeSeedCount), labelColor, valueGreen); }
        if (cadantineSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Cadantine seed", String.valueOf(cadantineSeedCount), labelColor, valueGreen); }
        if (kwuarmSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Kwuarm seed", String.valueOf(kwuarmSeedCount), labelColor, valueGreen); }
        if (avantoeSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Avantoe seed", String.valueOf(avantoeSeedCount), labelColor, valueGreen); }
        if (iritSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Irit seed", String.valueOf(iritSeedCount), labelColor, valueGreen); }
        if (toadflaxSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Toadflax seed", String.valueOf(toadflaxSeedCount), labelColor, valueGreen); }
        if (harralanderSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Harralander seed", String.valueOf(harralanderSeedCount), labelColor, valueGreen); }
        if (tarrominSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Tarromin seed", String.valueOf(tarrominSeedCount), labelColor, valueGreen); }
        if (marrentillSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Marrentill seed", String.valueOf(marrentillSeedCount), labelColor, valueGreen); }
        if (guamSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Guam seed", String.valueOf(guamSeedCount), labelColor, valueGreen); }
        if (dwarfWeedSeedCount > 0) { curY += lineGap; drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Dwarf weed seed", String.valueOf(dwarfWeedSeedCount), labelColor, valueGreen); }
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
}
