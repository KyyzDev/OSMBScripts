package main;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import tasks.*;
import utils.Task;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@ScriptDefinition(
        name = "Kyyz Master Farmer",
        description = "Pickpockets Master Farmer for herb seeds with proper OSMB flow",
        skillCategory = SkillCategory.THIEVING,
        version = 1.1,
        author = "Kyyz"
)
public class KyyzMasterFarmer extends Script {
    public static final String scriptVersion = "1.1";
    public static boolean setupDone = false;
    public static String task = "Initialize";
    public static int pickpocketCount = 0;
    public static int successfulPickpockets = 0;
    public static int failedPickpockets = 0;
    public static long startTime = System.currentTimeMillis();
    public static long totalGpValue = 0;
    private static java.util.Map<Integer, Integer> seedPrices = new java.util.HashMap<>();
    private static long lastPriceUpdate = 0;
    private static final long PRICE_UPDATE_INTERVAL = 300_000L;
    public static long lastSuccessfulAction = System.currentTimeMillis();
    public static boolean chatDetected = false;
    public static boolean hasAttemptedPickpocket = false;

    
    public static int guamSeedCount = 0;
    public static int marrentillSeedCount = 0;
    public static int tarrominSeedCount = 0;
    public static int harralanderSeedCount = 0;
    public static int ranarrSeedCount = 0;
    public static int toadflaxSeedCount = 0;
    public static int iritSeedCount = 0;
    public static int avantoeSeedCount = 0;
    public static int kwuarmSeedCount = 0;
    public static int snapdragonSeedCount = 0;
    public static int cadantineSeedCount = 0;
    public static int lantadymeSeedCount = 0;
    public static int dwarfWeedSeedCount = 0;
    public static int torstolSeedCount = 0;
    public static int limpwurtSeedCount = 0;
    public static int watermelonSeedCount = 0;
    public static int snapeGrassSeedCount = 0;
    public static int seaweedSporeCount = 0;     

    public static String targetNPC = "Master Farmer";
    public static boolean eatFood = true;
    public static int hpThreshold = 50;
    public static String lootMode = "DROP_ALL";
    public static String foodName = "Lobster";
    public static int foodItemId = 379;
    public static int foodAmount = 4;
    public static boolean needFood = false;
    public static boolean useSeedBox = false;
    public static boolean needSeedBox = false;
    public static int seedBoxFillCounter = 0;
    public static int seedBoxFillThreshold = 5 + (int)(Math.random() * 11);
    public static final int SEED_BOX_EMPTY = 13639;
    public static final int SEED_BOX_OPEN = 24482;
    public static final java.util.Set<Integer> SEED_BOX_IDS = java.util.Set.of(SEED_BOX_EMPTY, SEED_BOX_OPEN);

    public static final int MASTER_FARMER_XP = 43;
    public static int totalThievingXP = 0;

    public static java.util.Set<Integer> getFoodIds(int baseFoodId) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        ids.add(baseFoodId);

        if (baseFoodId == 1891) { ids.add(1893); ids.add(1895); }
        else if (baseFoodId == 1897) { ids.add(1899); ids.add(1901); }
        else if (baseFoodId == 2289) { ids.add(2291); }
        else if (baseFoodId == 2293) { ids.add(2295); }
        else if (baseFoodId == 2297) { ids.add(2299); }
        else if (baseFoodId == 2301) { ids.add(2303); }
        else if (baseFoodId == 2323) { ids.add(2335); }
        else if (baseFoodId == 2325) { ids.add(2333); }
        else if (baseFoodId == 2327) { ids.add(2331); }
        else if (baseFoodId == 7178) { ids.add(7180); }
        else if (baseFoodId == 7188) { ids.add(7190); }
        else if (baseFoodId == 7198) { ids.add(7200); }
        else if (baseFoodId == 7208) { ids.add(7210); }
        else if (baseFoodId == 7218) { ids.add(7220); }

        return ids;
    }

    public static int getFoodHealAmount(int baseFoodId) {
        switch (baseFoodId) {
            case 379: return 12;          // Lobster
            case 385: return 16;          // Shark
            case 391: return 20;          // Manta ray
            case 3144: return 18;         // Karambwan
            case 7946: return 22;         // Monkfish
            case 361: return 8;           // Tuna
            case 373: return 6;           // Swordfish
            case 1891: return 4;          // Cake (per slice)
            case 1897: return 5;          // Chocolate cake (per slice)
            case 2289: return 6;          // Plain pizza (per half)
            case 2293: return 7;          // Meat pizza (per half)
            case 2297: return 8;          // Anchovy pizza (per half)
            case 2301: return 9;          // Pineapple pizza (per half)
            case 2323: return 5;          // Pie (per half)
            case 2325: return 6;          // Redberry pie (per half)
            case 2327: return 6;          // Meat pie (per half)
            case 7178: return 8;          // Garden pie (per half)
            case 7188: return 11;         // Fish pie (per half)
            case 7198: return 12;         // Admiral pie (per half)
            case 7208: return 11;         // Wild pie (per half)
            case 7218: return 11;         // Summer pie (per half)
            default: return 10;           // Default fallback
        }
    }

    public static boolean bankIsFull = false;
    public static int dropThreshold = 24 + (int)(Math.random() * 5);

    public static ScriptUI.Location selectedLocation = ScriptUI.Location.DRAYNOR;
    public static com.osmb.api.location.position.types.WorldPosition farmerLocation =
        new com.osmb.api.location.position.types.WorldPosition(3079, 3250, 0);
    public static com.osmb.api.location.position.types.WorldPosition centerTile =
        new com.osmb.api.location.position.types.WorldPosition(3081, 3249, 0);

    public static boolean webhookEnabled = false;
    public static boolean webhookShowUser = false;
    private static String webhookUrl = "";
    public static int webhookIntervalMinutes = 5;
    public static long lastWebhookSent = 0;
    private boolean webhookCurrentlySending = false;
    private long webhookCooldownUntil = 0L;
    private Image savedScreenshot = null;
    private static String user = "";

    private List<Task> tasks;


    private static final Font FONT_TITLE = new Font("Small Fonts", Font.BOLD, 14);
    private static final Font FONT_LABEL = new Font("Small Fonts", Font.PLAIN, 10);

    public KyyzMasterFarmer(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                12338,
                12598,
                12342,
                10806,
                6965,
                10288,
                4922,
                5178
        };
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;  
    }

    @Override
    public void onStart() {
        resetStaticState();

        log("INFO", "Starting Kyyz Master Farmer v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        javafx.scene.Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Kyyz Master Farmer", true);

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            log("WEBHOOK", "Webhook enabled. Interval: " + webhookIntervalMinutes + "min.");
            queueSendWebhook();
        }

        updateSeedPrices();

        selectedLocation = ui.getLocation();
        farmerLocation = new com.osmb.api.location.position.types.WorldPosition(
            selectedLocation.getFarmerX(),
            selectedLocation.getFarmerY(),
            selectedLocation.getFarmerZ()
        );
        centerTile = new com.osmb.api.location.position.types.WorldPosition(
            selectedLocation.getCenterX(),
            selectedLocation.getCenterY(),
            selectedLocation.getFarmerZ()
        );

        targetNPC = "Master Farmer";
        ScriptUI.FoodType selectedFood = ui.getFoodType();

        
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
        log("INFO", "  Location: " + selectedLocation.getDisplayName());
        log("INFO", "  Target: " + targetNPC);
        log("INFO", "  Food: " + (eatFood ? foodAmount + "x " + foodName + " @ " + hpThreshold + " HP" : "Disabled"));
        log("INFO", "  Loot: " + lootMode);
        log("INFO", "  Seed Box: " + (useSeedBox ? "Enabled" : "Disabled"));
        log("INFO", "===========================================");

        tasks = Arrays.asList(
                new Setup(this),
                new tasks.ChatboxTask(this),
                new EatTask(this),
                new BankTask(this),
                new WalkTask(this),
                new ManageLootTask(this),
                new ThieveTask(this)
        );

        log("INFO", "Script initialized!");
    }

    @Override
    public int poll() {
        if (!setupDone) {
            try {
                if (getWidgetManager().getChatbox().getUsername() == null) {
                    task = "Waiting for login";
                    return 1000;
                }
            } catch (Exception e) {
                task = "Waiting for client";
                return 1000;
            }
        }

        if (setupDone && System.currentTimeMillis() - lastSuccessfulAction > 300_000) {
            log("WARN", "========================================");
            log("WARN", "  NO PROGRESS FOR 5 MINUTES!");
            log("WARN", "  Script appears stuck - logging out safely");
            log("WARN", "========================================");
            safeLogout("Stuck/AFK detected");
            return 1000;
        }

        if (setupDone && eatFood) {
            Integer currentHp = getWidgetManager().getMinimapOrbs().getHitpoints();
            if (currentHp != null && currentHp <= 10) {
                java.util.Set<Integer> allFoodIds = getFoodIds(foodItemId);
                com.osmb.api.item.ItemGroupResult inv = getWidgetManager().getInventory().search(allFoodIds);
                int foodCount = (inv != null && !inv.isEmpty()) ? inv.getAmount(allFoodIds) : 0;

                if (foodCount == 0) {
                    log("WARN", "========================================");
                    log("WARN", "  CRITICAL HP (" + currentHp + ") WITH NO FOOD!");
                    log("WARN", "  Emergency logout to prevent death");
                    log("WARN", "========================================");
                    safeLogout("Critical HP - no food");
                    return 1000;
                }
            }
        }

        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            queueSendWebhook();
        }

        if (System.currentTimeMillis() - lastPriceUpdate >= PRICE_UPDATE_INTERVAL) {
            updateSeedPrices();
        }

        calculateTotalGp();

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

    private void safeLogout(String reason) {
        task = "Emergency logout: " + reason;
        try {
            getWidgetManager().getLogoutTab().logout();
            submitTask(() -> false, 3000);
        } catch (Exception e) {
            log("WARN", "Failed to logout: " + e.getMessage());
        }
        stop();
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        String runtime = getTimeString(elapsed);

        double hours = Math.max(1e-9, elapsed / 3_600_000.0);
        int pickpocketsPerHour = (int) Math.round(successfulPickpockets / hours);
        int gpPerHour = (int) Math.round(totalGpValue / hours);
        int xpPerHour = (int) Math.round(totalThievingXP / hours);

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
        if (limpwurtSeedCount > 0) seedTypesCollected++;
        if (watermelonSeedCount > 0) seedTypesCollected++;
        if (snapeGrassSeedCount > 0) seedTypesCollected++;
        if (seaweedSporeCount > 0) seedTypesCollected++;

        int totalLines = 10 + seedTypesCollected;
        int innerHeight = titleHeight + (totalLines * lineGap) + topGap + 8;

        
        
        c.fillRect(innerX - 2, innerY - 2, innerWidth + 4, innerHeight + 4, osrsBorder.getRGB(), 1);

        
        c.fillRect(innerX, innerY, innerWidth, innerHeight, osrsBrown.getRGB(), 1);

        
        c.fillRect(innerX, innerY, innerWidth, titleHeight, new Color(52, 43, 31, 240).getRGB(), 1);

        
        String title = "Kyyz Master Farmer";
        int titleX = innerX + (innerWidth / 2) - (c.getFontMetrics(FONT_TITLE).stringWidth(title) / 2);
        int titleY = innerY + 26;
        c.drawText(title, titleX, titleY, valueYellow, FONT_TITLE);

        
        int sepY = innerY + titleHeight;
        c.fillRect(innerX, sepY, innerWidth, 1, osrsBorder.getRGB(), 1);

        int curY = innerY + titleHeight + topGap + lineGap;

        
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Runtime", runtime, labelColor, labelColor);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Successful", String.valueOf(successfulPickpockets), labelColor, valueGreen);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Failed", String.valueOf(failedPickpockets), labelColor, osrsOrange.getRGB());

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Per hour", String.valueOf(pickpocketsPerHour), labelColor, valueYellow);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "XP gained", formatNumber(totalThievingXP), labelColor, valueGreen);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "XP/hr", formatNumber(xpPerHour), labelColor, valueYellow);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "GP earned", formatGold(totalGpValue), labelColor, valueGreen);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "GP/hour", formatGold(gpPerHour), labelColor, valueYellow);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Task", task, labelColor, labelColor);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Version", scriptVersion, labelColor, labelColor);

        
        if (ranarrSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Ranarr seed", String.valueOf(ranarrSeedCount), labelColor, valueGreen); }
        if (snapdragonSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Snapdragon seed", String.valueOf(snapdragonSeedCount), labelColor, valueGreen); }
        if (torstolSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Torstol seed", String.valueOf(torstolSeedCount), labelColor, valueGreen); }
        if (lantadymeSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Lantadyme seed", String.valueOf(lantadymeSeedCount), labelColor, valueGreen); }
        if (cadantineSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Cadantine seed", String.valueOf(cadantineSeedCount), labelColor, valueGreen); }
        if (kwuarmSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Kwuarm seed", String.valueOf(kwuarmSeedCount), labelColor, valueGreen); }
        if (avantoeSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Avantoe seed", String.valueOf(avantoeSeedCount), labelColor, valueGreen); }
        if (iritSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Irit seed", String.valueOf(iritSeedCount), labelColor, valueGreen); }
        if (toadflaxSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Toadflax seed", String.valueOf(toadflaxSeedCount), labelColor, valueGreen); }
        if (harralanderSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Harralander seed", String.valueOf(harralanderSeedCount), labelColor, valueGreen); }
        if (tarrominSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Tarromin seed", String.valueOf(tarrominSeedCount), labelColor, valueGreen); }
        if (marrentillSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Marrentill seed", String.valueOf(marrentillSeedCount), labelColor, valueGreen); }
        if (guamSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Guam seed", String.valueOf(guamSeedCount), labelColor, valueGreen); }
        if (dwarfWeedSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Dwarf weed seed", String.valueOf(dwarfWeedSeedCount), labelColor, valueGreen); }
        if (limpwurtSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Limpwurt seed", String.valueOf(limpwurtSeedCount), labelColor, valueGreen); }
        if (watermelonSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Watermelon seed", String.valueOf(watermelonSeedCount), labelColor, valueGreen); }
        if (snapeGrassSeedCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Snape grass seed", String.valueOf(snapeGrassSeedCount), labelColor, valueGreen); }
        if (seaweedSporeCount > 0) { curY += lineGap; drawStatLine(c, innerX, innerWidth, paddingX, curY, "Seaweed spore", String.valueOf(seaweedSporeCount), labelColor, valueGreen); }

        if (hasAttemptedPickpocket && !chatDetected && setupDone) {
            curY += lineGap + 5;
            c.fillRect(innerX, curY - 12, innerWidth, 36, new Color(139, 0, 0, 220).getRGB(), 1);
            String warning1 = "Right-click 'Game' chat";
            int warningX1 = innerX + (innerWidth / 2) - (c.getFontMetrics(FONT_LABEL).stringWidth(warning1) / 2);
            c.drawText(warning1, warningX1, curY + 2, osrsYellow.getRGB(), FONT_LABEL);
            String warning2 = "Set to: On (not Filtered)";
            int warningX2 = innerX + (innerWidth / 2) - (c.getFontMetrics(FONT_LABEL).stringWidth(warning2) / 2);
            c.drawText(warning2, warningX2, curY + 18, osrsYellow.getRGB(), FONT_LABEL);
        }

        try {
            savedScreenshot = c.toImageCopy();
        } catch (Exception e) {
            log("WARN", "Failed to capture screenshot: " + e.getMessage());
        }
    }

    private void drawStatLine(Canvas c, int x, int width, int padding, int yPos,
                              String leftText, String rightText, int leftCol, int rightCol) {
        c.drawText(leftText, x + padding, yPos, leftCol, FONT_LABEL);
        int rightWidth = c.getFontMetrics(FONT_LABEL).stringWidth(rightText);
        c.drawText(rightText, x + width - padding - rightWidth, yPos, rightCol, FONT_LABEL);
    }

    private String getTimeString(long ms) {
        long totalSeconds = ms / 1000;
        int h = (int)(totalSeconds / 3600);
        int m = (int)((totalSeconds % 3600) / 60);
        int s = (int)(totalSeconds % 60);
        return h + "h " + m + "m " + s + "s";
    }

    private String formatGold(long amount) {
        if (amount < 1000) return amount + " gp";
        if (amount < 1000000) return (amount / 1000) + "." + ((amount % 1000) / 100) + "k";
        return (amount / 1000000) + "." + ((amount % 1000000) / 100000) + "m";
    }

    private String formatNumber(int amount) {
        if (amount < 1000) return String.valueOf(amount);
        if (amount < 1000000) return (amount / 1000) + "." + ((amount % 1000) / 100) + "k";
        return (amount / 1000000) + "." + ((amount % 1000000) / 100000) + "m";
    }

    private void postDiscordUpdate() {
        if (savedScreenshot == null) {
            log("WARN", "No screenshot available for webhook, skipping");
            webhookCurrentlySending = false;
            return;
        }

        try {
            byte[] pngData = createPngBytes(savedScreenshot);
            String jsonPayload = buildWebhookJson();
            int statusCode = sendMultipartWebhook(jsonPayload, pngData);

            handleWebhookResponse(statusCode);
        } catch (Exception e) {
            log("WARN", "Webhook failed: " + e.getMessage());
            webhookCurrentlySending = false;
        }
    }

    private byte[] createPngBytes(Image img) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(img.toBufferedImage(), "png", stream);
        byte[] data = stream.toByteArray();
        stream.close();
        return data;
    }

    private String buildWebhookJson() {
        String displayName = webhookShowUser && user != null && !user.isEmpty() ? user : "Player";
        String elapsed = getTimeString(System.currentTimeMillis() - startTime);

        long futureTime = System.currentTimeMillis() + (webhookIntervalMinutes * 60000);
        String nextUpdate = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(futureTime),
                            ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"));

        double hours = Math.max(1e-9, (System.currentTimeMillis() - startTime) / 3_600_000.0);
        int successPerHour = (int) Math.round(successfulPickpockets / hours);
        int gpPerHour = (int) Math.round(totalGpValue / hours);

        int totalAttempts = successfulPickpockets + failedPickpockets;
        double successRate = totalAttempts > 0 ? (successfulPickpockets * 100.0 / totalAttempts) : 0;

        StringBuilder desc = new StringBuilder();
        desc.append("## ⏱️ Session Stats\\n");
        desc.append("**Runtime:** ").append(elapsed).append("\\n");
        desc.append("**Player:** ").append(displayName).append("\\n\\n");

        desc.append("## 💰 Earnings\\n");
        desc.append("**Total GP:** ").append(formatGold(totalGpValue)).append("\\n");
        desc.append("**GP/Hour:** ").append(formatGold(gpPerHour)).append("\\n\\n");

        desc.append("## 🎯 Pickpocket Stats\\n");
        desc.append("**Successful:** ").append(successfulPickpockets);
        desc.append(" (").append(successPerHour).append("/hr)\\n");
        desc.append("**Failed:** ").append(failedPickpockets).append("\\n");
        desc.append("**Success Rate:** ").append(String.format("%.1f", successRate)).append("%\\n\\n");

        desc.append("## 🌱 Seeds Collected\\n");
        if (ranarrSeedCount > 0) desc.append("**Ranarr:** ").append(ranarrSeedCount).append("\\n");
        if (snapdragonSeedCount > 0) desc.append("**Snapdragon:** ").append(snapdragonSeedCount).append("\\n");
        if (toadflaxSeedCount > 0) desc.append("**Toadflax:** ").append(toadflaxSeedCount).append("\\n");
        if (iritSeedCount > 0) desc.append("**Irit:** ").append(iritSeedCount).append("\\n");
        if (avantoeSeedCount > 0) desc.append("**Avantoe:** ").append(avantoeSeedCount).append("\\n");
        if (kwuarmSeedCount > 0) desc.append("**Kwuarm:** ").append(kwuarmSeedCount).append("\\n");
        if (cadantineSeedCount > 0) desc.append("**Cadantine:** ").append(cadantineSeedCount).append("\\n");
        if (lantadymeSeedCount > 0) desc.append("**Lantadyme:** ").append(lantadymeSeedCount).append("\\n");
        if (dwarfWeedSeedCount > 0) desc.append("**Dwarf weed:** ").append(dwarfWeedSeedCount).append("\\n");
        if (harralanderSeedCount > 0) desc.append("**Harralander:** ").append(harralanderSeedCount).append("\\n");
        if (tarrominSeedCount > 0) desc.append("**Tarromin:** ").append(tarrominSeedCount).append("\\n");
        if (marrentillSeedCount > 0) desc.append("**Marrentill:** ").append(marrentillSeedCount).append("\\n");
        if (guamSeedCount > 0) desc.append("**Guam:** ").append(guamSeedCount).append("\\n");

        desc.append("\\n📊 **Share your proggy:** https://discord.com/channels/736938454478356570/789791439487500299");

        return "{\"embeds\":[{" +
               "\"title\":\"🌾 Kyyz Master Farmer\"," +
               "\"description\":\"" + desc.toString() + "\"," +
               "\"color\":5763719," +
               "\"thumbnail\":{\"url\":\"https://oldschool.runescape.wiki/images/Master_Farmer.png\"}," +
               "\"image\":{\"url\":\"attachment://progress.png\"}," +
               "\"footer\":{\"text\":\"⏰ Next update at " + nextUpdate + "\",\"icon_url\":\"https://i.imgur.com/fKMAelP.png\"}" +
               "}]}";
    }

    private int sendMultipartWebhook(String json, byte[] imageData) throws Exception {
        String boundary = "KyyzFormBoundary" + System.currentTimeMillis();
        byte[] CRLF = "\r\n".getBytes();
        byte[] DASHES = "--".getBytes();

        HttpURLConnection http = (HttpURLConnection) new URL(webhookUrl).openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();

        bodyStream.write(DASHES);
        bodyStream.write(boundary.getBytes());
        bodyStream.write(CRLF);
        bodyStream.write("Content-Disposition: form-data; name=\"payload_json\"\r\n\r\n".getBytes());
        bodyStream.write(json.getBytes(StandardCharsets.UTF_8));
        bodyStream.write(CRLF);

        bodyStream.write(DASHES);
        bodyStream.write(boundary.getBytes());
        bodyStream.write(CRLF);
        bodyStream.write("Content-Disposition: form-data; name=\"file\"; filename=\"progress.png\"\r\n".getBytes());
        bodyStream.write("Content-Type: image/png\r\n\r\n".getBytes());
        bodyStream.write(imageData);
        bodyStream.write(CRLF);

        bodyStream.write(DASHES);
        bodyStream.write(boundary.getBytes());
        bodyStream.write(DASHES);
        bodyStream.write(CRLF);

        byte[] fullBody = bodyStream.toByteArray();
        bodyStream.close();

        OutputStream outStream = http.getOutputStream();
        outStream.write(fullBody);
        outStream.flush();
        outStream.close();

        return http.getResponseCode();
    }

    private void handleWebhookResponse(int code) {
        if (code == 200 || code == 204) {
            log("WEBHOOK", "Webhook posted successfully!");
            lastWebhookSent = System.currentTimeMillis();
            webhookCurrentlySending = false;
        } else if (code == 429) {
            log("WARN", "Webhook rate limited (429)");
            webhookCurrentlySending = false;
            webhookCooldownUntil = System.currentTimeMillis() + 30000;
        } else {
            log("WARN", "Webhook failed with response code: " + code);
            webhookCurrentlySending = false;
        }
    }

    public void queueSendWebhook() {
        if (!webhookEnabled || webhookUrl == null || webhookUrl.isEmpty()) {
            log("WARN", "Webhook not enabled or URL empty");
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (webhookCurrentlySending) {
            log("INFO", "Webhook already sending, skipping");
            return;
        }
        if (currentTime < webhookCooldownUntil) {
            log("INFO", "Webhook on cooldown, skipping");
            return;
        }
        if (currentTime - lastWebhookSent < webhookIntervalMinutes * 60000) {
            log("INFO", "Webhook interval not met, skipping");
            return;
        }

        log("WEBHOOK", "Sending webhook update...");
        webhookCurrentlySending = true;

        Thread webhookThread = new Thread(() -> postDiscordUpdate());
        webhookThread.setDaemon(true);
        webhookThread.start();
    }

    private void updateSeedPrices() {
        Thread t = new Thread(() -> {
            try {
                int[] seedIds = {5291, 5292, 5293, 5294, 5295, 5296, 5297, 5298, 5299, 5300, 5301, 5302, 5303, 5304, 5100, 5321, 22879, 21490};

                for (int seedId : seedIds) {
                    try {
                        URL url = new URL("https://www.ge-tracker.com/api/items/" + seedId);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();

                        String json = response.toString();

                        int sellingIdx = json.indexOf("\"selling\":");
                        if (sellingIdx != -1) {
                            int start = sellingIdx + 10;
                            int end = json.indexOf(",", start);
                            if (end == -1) end = json.indexOf("}", start);
                            if (end != -1) {
                                String priceStr = json.substring(start, end).trim();
                                try {
                                    int price = Integer.parseInt(priceStr);
                                    seedPrices.put(seedId, price);
                                } catch (NumberFormatException ignored) {}
                            }
                        }

                        Thread.sleep(100);

                    } catch (Exception e) {
                    }
                }

                lastPriceUpdate = System.currentTimeMillis();
                log("PRICES", "Updated seed prices from GE Tracker");

            } catch (Exception e) {
                log("PRICES", "Failed to update prices: " + e.getMessage());
            }
        }, "PriceUpdater");
        t.setDaemon(true);
        t.start();
    }

    private void resetStaticState() {
        setupDone = false;
        task = "Initialize";
        pickpocketCount = 0;
        successfulPickpockets = 0;
        failedPickpockets = 0;
        startTime = System.currentTimeMillis();
        totalGpValue = 0;
        lastSuccessfulAction = System.currentTimeMillis();
        chatDetected = false;
        hasAttemptedPickpocket = false;

        guamSeedCount = 0;
        marrentillSeedCount = 0;
        tarrominSeedCount = 0;
        harralanderSeedCount = 0;
        ranarrSeedCount = 0;
        toadflaxSeedCount = 0;
        iritSeedCount = 0;
        avantoeSeedCount = 0;
        kwuarmSeedCount = 0;
        snapdragonSeedCount = 0;
        cadantineSeedCount = 0;
        lantadymeSeedCount = 0;
        dwarfWeedSeedCount = 0;
        torstolSeedCount = 0;
        limpwurtSeedCount = 0;
        watermelonSeedCount = 0;
        snapeGrassSeedCount = 0;
        seaweedSporeCount = 0;

        needFood = false;
        needSeedBox = false;
        seedBoxFillCounter = 0;
        seedBoxFillThreshold = 5 + (int)(Math.random() * 11);
        bankIsFull = false;
        dropThreshold = 24 + (int)(Math.random() * 5);
        totalThievingXP = 0;
        lastWebhookSent = 0;

        selectedLocation = ScriptUI.Location.DRAYNOR;
        farmerLocation = new com.osmb.api.location.position.types.WorldPosition(3079, 3250, 0);
        centerTile = new com.osmb.api.location.position.types.WorldPosition(3081, 3249, 0);
    }

    private void calculateTotalGp() {
        totalGpValue = 0;
        totalGpValue += guamSeedCount * seedPrices.getOrDefault(5291, 0);
        totalGpValue += marrentillSeedCount * seedPrices.getOrDefault(5292, 0);
        totalGpValue += tarrominSeedCount * seedPrices.getOrDefault(5293, 0);
        totalGpValue += harralanderSeedCount * seedPrices.getOrDefault(5294, 0);
        totalGpValue += ranarrSeedCount * seedPrices.getOrDefault(5295, 0);
        totalGpValue += toadflaxSeedCount * seedPrices.getOrDefault(5296, 0);
        totalGpValue += iritSeedCount * seedPrices.getOrDefault(5297, 0);
        totalGpValue += avantoeSeedCount * seedPrices.getOrDefault(5298, 0);
        totalGpValue += kwuarmSeedCount * seedPrices.getOrDefault(5299, 0);
        totalGpValue += snapdragonSeedCount * seedPrices.getOrDefault(5300, 0);
        totalGpValue += cadantineSeedCount * seedPrices.getOrDefault(5301, 0);
        totalGpValue += lantadymeSeedCount * seedPrices.getOrDefault(5302, 0);
        totalGpValue += dwarfWeedSeedCount * seedPrices.getOrDefault(5303, 0);
        totalGpValue += torstolSeedCount * seedPrices.getOrDefault(5304, 0);
        totalGpValue += limpwurtSeedCount * seedPrices.getOrDefault(5100, 0);
        totalGpValue += watermelonSeedCount * seedPrices.getOrDefault(5321, 0);
        totalGpValue += snapeGrassSeedCount * seedPrices.getOrDefault(22879, 0);
        totalGpValue += seaweedSporeCount * seedPrices.getOrDefault(21490, 0);
    }
}
