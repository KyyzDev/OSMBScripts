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
    
    public static final int ATLATL_DART_TIP = 30998;
    
    private static final double XP_BRONZE     = 1.8;
    private static final double XP_IRON       = 3.8;
    private static final double XP_STEEL      = 7.5;
    private static final double XP_MITHRIL    = 11.2;
    private static final double XP_ADAMANT    = 15.0;
    private static final double XP_RUNE       = 18.8;
    private static final double XP_AMETHYST   = 21.0;
    private static final double XP_DRAGON     = 25.0;
    private static final double XP_ATLATL     = 25.0; 

    public static boolean setupDone   = false;
    public static boolean hasReqs     = true;
    public static boolean shouldBank  = false; 
    
    public static int FeatherID        = ItemID.FEATHER;
    public static int HeadlessAtlatlID = ItemID.HEADLESS_ATLATL_DART;


    public static int PrimaryIngredientID = FeatherID;
    
    public static int SelectedDartTipID = ItemID.MITHRIL_DART_TIP;
    
    public static int dartID = ItemID.MITHRIL_DART;
    
    public static int craftCount = 0;
    public static String task    = "Initialize";
    public static long startTime = System.currentTimeMillis();
    
    public static int tapSpeed = 50;

    private List<Task> tasks;
    private static final Font ARIEL = Font.getFont("Arial");
    
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
        
        try {
            ScriptUI ui = new ScriptUI(this);
            Scene scene = ui.buildScene(this);
            getStageController().show(scene, "Dart Options", false);

            int selectedTip = ui.getSelectedDartID();
            SelectedDartTipID = selectedTip;
            dartID = mapTipToDart(selectedTip);
            
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
        
        double xpPerDart = xpPerDartForTip(SelectedDartTipID);
        
        int totalXp    = (int)Math.round(craftCount * xpPerDart);
        int dartsPerHr = (int)((craftCount * 3_600_000L) / elapsed);
        int xpPerHr    = (int)((totalXp   * 3_600_000L) / elapsed);

        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);
        
        int panelX = 5;
        int panelY = 40;
        int panelW = 260;
        int panelH = 170;
        
        c.fillRect(panelX, panelY, panelW, panelH, Color.BLACK.getRGB(), 0.75f);
        c.drawRect(panelX, panelY, panelW, panelH, Color.BLACK.getRGB());
        
        int headerPad = 8; 
        int headerH   = 32;  
        int headerX   = panelX + headerPad;
        int headerY   = panelY + headerPad;
        int headerW   = panelW - headerPad * 2;
        
        drawHeader(c, headerX, headerY, headerW, headerH, "AIO Dart Maker");
        
        int y = headerY + headerH + 14;

        c.drawText("Darts made: " + f.format(craftCount), panelX + 10, y, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Darts/hr: "   + f.format(dartsPerHr), panelX + 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP gained: "  + f.format(totalXp),    panelX + 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("XP/hr: "      + f.format(xpPerHr),    panelX + 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Task: "       + task,                 panelX + 10, y += 20, Color.WHITE.getRGB(), ARIEL);
    }
    
    private int mapTipToDart(int tipId) {
        if (tipId == ItemID.BRONZE_DART_TIP)   return ItemID.BRONZE_DART;
        if (tipId == ItemID.IRON_DART_TIP)     return ItemID.IRON_DART;
        if (tipId == ItemID.STEEL_DART_TIP)    return ItemID.STEEL_DART;
        if (tipId == ItemID.MITHRIL_DART_TIP)  return ItemID.MITHRIL_DART;
        if (tipId == ItemID.ADAMANT_DART_TIP)  return ItemID.ADAMANT_DART;
        if (tipId == ItemID.RUNE_DART_TIP)     return ItemID.RUNE_DART;
        if (tipId == ItemID.DRAGON_DART_TIP)   return ItemID.DRAGON_DART;
        if (tipId == ItemID.AMETHYST_DART_TIP) return ItemID.AMETHYST_DART;
        
        if (tipId == ATLATL_DART_TIP) {
            try {
                return (int) ItemID.class.getField("ATLATL_DART").get(null);
            } catch (Throwable ignored) {
                return ItemID.MITHRIL_DART;
            }
        }
        
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
        if (tipId == ATLATL_DART_TIP)          return XP_ATLATL;
        return XP_MITHRIL; 
    }
    
    private void drawHeader(Canvas c, int x, int y, int w, int h, String title) {
        c.fillRect(x, y, w, h, HEADER_DARK, 0.95f);
        c.drawRect(x, y, w, h, HEADER_BORDER);
        c.drawRect(x + 2, y + 2, w - 4, h - 4, HEADER_GOLD_DK);
        
        int approxCharW = 7;
        int textW = Math.max(title.length() * approxCharW, 0);
        int tx = x + Math.max((w - textW) / 2, 6);
        int ty = y + (h / 2) + 5;
        
        c.drawText(title, tx + 1, ty + 1, HEADER_BORDER, ARIEL);
        c.drawText(title, tx, ty, HEADER_GOLD, ARIEL);
    }

    private String safeName(int itemId) {
        try { return getItemManager().getItemName(itemId); }
        catch (Throwable t) { return String.valueOf(itemId); }
    }
}
