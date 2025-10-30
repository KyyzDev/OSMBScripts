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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(
        name = "Kyyz Master Farmer",
        description = "Pickpockets Master Farmer for herb seeds",
        skillCategory = SkillCategory.THIEVING,
        version = 1.0,
        author = "Kyyz"
)
public class KyyzMasterFarmer extends Script implements WebhookSender {
    public static final String scriptVersion = "1.0";
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
    public static boolean chatDetected = true;

    
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

    public static String targetNPC = "Master Farmer";
    public static boolean eatFood = true;
    public static int hpThreshold = 50;  
    public static String lootMode = "DROP_ALL";
    public static String foodName = "Lobster";
    public static int foodItemId = 379;  
    public static int foodAmount = 4;  
    public static boolean needFood = false;
    public static boolean useSeedBox = false;
    public static final int SEED_BOX_ID = 13639;
    public static boolean bankIsFull = false;

    public static final com.osmb.api.location.position.types.WorldPosition DRAYNOR_FARMER_LOCATION =
        new com.osmb.api.location.position.types.WorldPosition(3079, 3250, 0);

    public static boolean webhookEnabled = false;
    public static boolean webhookShowUser = false;
    private static String webhookUrl = "";
    public static int webhookIntervalMinutes = 5;
    public static long lastWebhookSent = 0;
    private final AtomicBoolean webhookInFlight = new AtomicBoolean(false);
    private volatile long nextWebhookEarliestMs = 0L;
    private final AtomicReference<Image> lastCanvasFrame = new AtomicReference<>();
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
                12598,
                12342,
                10806,
                6965,
                10288,
                4922
        };
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;  
    }

    @Override
    public void onStart() {
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

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        String runtime = formatRuntime(elapsed);

        double hours = Math.max(1e-9, elapsed / 3_600_000.0);
        int pickpocketsPerHour = (int) Math.round(pickpocketCount / hours);
        int gpPerHour = (int) Math.round(totalGpValue / hours);

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

        int totalLines = 8 + seedTypesCollected;
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

        
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Runtime", runtime, labelColor, labelColor);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Successful", String.valueOf(successfulPickpockets), labelColor, valueGreen);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Failed", String.valueOf(failedPickpockets), labelColor, osrsOrange.getRGB());

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Per hour", String.valueOf(pickpocketsPerHour), labelColor, valueYellow);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "GP earned", formatGp(totalGpValue), labelColor, valueGreen);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "GP/hour", formatGp(gpPerHour), labelColor, valueYellow);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Task", task, labelColor, labelColor);

        curY += lineGap;
        drawOSRSStatLine(c, innerX, innerWidth, paddingX, curY, "Version", scriptVersion, labelColor, labelColor);

        
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

        try {
            lastCanvasFrame.set(c.toImageCopy());
        } catch (Exception ignored) {}
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

    private String formatGp(long gp) {
        if (gp >= 1_000_000) {
            return String.format("%.1fM", gp / 1_000_000.0);
        } else if (gp >= 1_000) {
            return String.format("%.1fK", gp / 1_000.0);
        } else {
            return String.valueOf(gp);
        }
    }

    private void sendWebhookInternal() {
        ByteArrayOutputStream imageStream = null;
        try {
            Image canvasSnapshot = lastCanvasFrame.get();
            if (canvasSnapshot == null) {
                return;
            }

            imageStream = new ByteArrayOutputStream();
            ImageIO.write(canvasSnapshot.toBufferedImage(), "png", imageStream);

            String displayName = webhookShowUser && user != null && !user.isEmpty() ? user : "Player";
            String runtimeDisplay = formatRuntime(System.currentTimeMillis() - startTime);

            ZonedDateTime nextUpdate = ZonedDateTime.now().plusMinutes(webhookIntervalMinutes);
            String nextTime = nextUpdate.format(DateTimeFormatter.ofPattern("HH:mm"));

            String embedJson = String.format(
                "{\"embeds\":[{\"title\":\"Kyyz Master Farmer - %s\"," +
                "\"description\":\"Runtime: **%s**\\nSuccessful: %d | Failed: %d\\nGP Earned: %s\\n\\n" +
                "Post your proggy: https://discord.com/channels/736938454478356570/789791439487500299\"," +
                "\"color\":5635925," +
                "\"image\":{\"url\":\"attachment://progress.png\"}," +
                "\"footer\":{\"text\":\"Next update: %s\"}}]}",
                displayName, runtimeDisplay, successfulPickpockets, failedPickpockets,
                formatGp(totalGpValue), nextTime
            );

            String multipartBoundary = "Boundary" + System.nanoTime();
            HttpURLConnection connection = (HttpURLConnection) new URL(webhookUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + multipartBoundary);

            try (OutputStream output = connection.getOutputStream()) {
                writeMultipartField(output, multipartBoundary, "payload_json", embedJson);
                writeMultipartFile(output, multipartBoundary, "progress.png", imageStream.toByteArray());
                output.write(("--" + multipartBoundary + "--\r\n").getBytes());
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200 || responseCode == 204) {
                lastWebhookSent = System.currentTimeMillis();
            } else if (responseCode == 429) {
                String retryAfter = connection.getHeaderField("Retry-After");
                long waitTime = 30000L;
                if (retryAfter != null) {
                    try {
                        waitTime = (long)(Double.parseDouble(retryAfter) * 1000);
                    } catch (NumberFormatException e) {}
                }
                nextWebhookEarliestMs = System.currentTimeMillis() + waitTime;
            }

        } catch (Exception e) {
        } finally {
            if (imageStream != null) {
                try { imageStream.close(); } catch (IOException e) {}
            }
            webhookInFlight.set(false);
        }
    }

    private void writeMultipartField(OutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes());
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes());
    }

    private void writeMultipartFile(OutputStream out, String boundary, String filename, byte[] data) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes());
        out.write("Content-Type: image/png\r\n\r\n".getBytes());
        out.write(data);
        out.write("\r\n".getBytes());
    }

    public void queueSendWebhook() {
        if (!webhookEnabled) return;

        long now = System.currentTimeMillis();
        if (now < nextWebhookEarliestMs) return;
        if (now - lastWebhookSent < webhookIntervalMinutes * 60_000L) return;

        if (!webhookInFlight.compareAndSet(false, true)) return;

        sendWebhookAsync();
    }

    public void sendWebhookAsync() {
        Thread t = new Thread(this::sendWebhookInternal, "WebhookSender");
        t.setDaemon(true);
        t.start();
    }

    private void updateSeedPrices() {
        Thread t = new Thread(() -> {
            try {
                int[] seedIds = {5291, 5292, 5293, 5294, 5295, 5296, 5297, 5298, 5299, 5300, 5301, 5302, 5303, 5304};

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
    }
}
