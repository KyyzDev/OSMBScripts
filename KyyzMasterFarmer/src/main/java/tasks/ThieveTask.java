package tasks;

import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.ui.tabs.Tab;
import utils.Task;

import java.awt.Point;
import java.util.List;

import static main.KyyzMasterFarmer.*;

public class ThieveTask extends Task {

    
    
    
    
    private static final SearchablePixel[] CYAN_HIGHLIGHT = new SearchablePixel[]{
            new SearchablePixel(-14155777, new SingleThresholdComparator(15), ColorModel.RGB),
            new SearchablePixel(-2171877, new SingleThresholdComparator(15), ColorModel.RGB),
            new SearchablePixel(0xFF05F8F8, new SingleThresholdComparator(15), ColorModel.RGB),
    };

    
    private java.util.Set<String> processedMessages = new java.util.HashSet<>();

    public ThieveTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone) {
            return false;
        }

        
        if (eatFood) {
            Integer currentHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();
            if (currentHp != null && currentHp <= hpThreshold) {
                script.log("DEBUG", "ThieveTask: HP is " + currentHp + " (threshold: " + hpThreshold + ") - NOT pickpocketing, letting EatTask handle it!");
                return false;  
            }
        }

        
        com.osmb.api.item.ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Collections.emptySet());
        if (inv != null && inv.isFull()) {
            script.log("DEBUG", "ThieveTask: Inventory is full - NOT pickpocketing, letting ManageLootTask handle it!");
            return false;  
        }

        return true;
    }

    @Override
    public boolean execute() {
        task = "Pickpocketing " + targetNPC;

        
        UIResultList<WorldPosition> npcPositions = script.getWidgetManager().getMinimap().getNPCPositions();

        if (npcPositions.isNotVisible() || npcPositions.isNotFound()) {
            script.log("WARN", "No NPCs found on minimap");
            return false;
        }

        
        for (WorldPosition position : npcPositions) {
            LocalPosition localPosition = position.toLocalPosition(script);
            if (localPosition == null) {
                continue;
            }

            
            Polygon poly = script.getSceneProjector().getTileCube(
                localPosition.getX(),
                localPosition.getY(),
                localPosition.getPlane(),
                150
            );

            if (poly == null) {
                continue;
            }

            
            Polygon resized = poly.getResized(0.8);
            Polygon cubeResized = (resized != null) ? resized.convexHull() : null;
            if (cubeResized == null) {
                cubeResized = poly;
            }

            
            List<Point> cyanPixels = script.getPixelAnalyzer().findPixelsOnGameScreen(cubeResized, CYAN_HIGHLIGHT);

            if (cyanPixels == null || cyanPixels.size() < 5) {
                continue; 
            }

            
            
            boolean tapped = script.getFinger().tapGameScreen(cubeResized, menuEntries -> {
                for (int i = 0; i < menuEntries.size(); i++) {
                    String action = menuEntries.get(i).getAction();
                    String entityName = menuEntries.get(i).getEntityName();

                    
                    if (action != null && entityName != null) {
                        String actionLower = action.toLowerCase();
                        String entityLower = entityName.toLowerCase();
                        String targetLower = targetNPC.toLowerCase();

                        if (actionLower.contains("pickpocket") && entityLower.equals(targetLower)) {
                            script.log("INFO", "✓ MATCH! Selecting: " + action + " " + entityName);
                            return menuEntries.get(i);
                        }
                    }
                }
                script.log("WARN", "No matching pickpocket entry found in menu!");
                return null;
            });

            if (tapped) {
                script.log("INFO", "✓ Successfully initiated pickpocket on " + targetNPC);
                pickpocketCount++;


                script.submitTask(() -> false, 600);


                trackPickpocketResultFromChat();


                if (useSeedBox) {
                    depositSeedsToSeedBox();
                }


                script.pollFramesUntil(() ->
                    !script.getPixelAnalyzer().isPlayerAnimating(0.3),
                    5000
                );

                if (processedMessages.size() > 200) {
                    processedMessages.clear();
                }

                return true;
            } else {
                script.log("WARN", "Failed to tap at position " + position + " - tapGameScreen returned false");
            }
        }

        script.log("WARN", "No " + targetNPC + " found nearby");
        return false;
    }

    private void trackPickpocketResultFromChat() {
        try {
            com.osmb.api.utils.UIResultList<String> chatMessages = script.getWidgetManager().getChatbox().getText();

            if (chatMessages == null || !chatMessages.isFound() || chatMessages.isEmpty()) {
                chatDetected = false;
                return;
            }

            chatDetected = true;


        for (String message : chatMessages) {
            if (message == null) {
                continue;
            }

            
            if (processedMessages.contains(message)) {
                continue;
            }

            String lowerMsg = message.toLowerCase();

            
            if (lowerMsg.contains("you steal")) {
                successfulPickpockets++;
                processedMessages.add(message);
            }
            
            else if (lowerMsg.contains("you fail") || lowerMsg.contains("failed to pick")) {
                failedPickpockets++;
                processedMessages.add(message);
            }

            
            if (!lowerMsg.contains("you steal")) {
                continue;
            }

            

            
            if (lowerMsg.contains("guam seed")) {
                int amount = parseAmount(message);
                guamSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Guam seed!");
            } else if (lowerMsg.contains("marrentill seed")) {
                int amount = parseAmount(message);
                marrentillSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Marrentill seed!");
            } else if (lowerMsg.contains("tarromin seed")) {
                int amount = parseAmount(message);
                tarrominSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Tarromin seed!");
            } else if (lowerMsg.contains("harralander seed")) {
                int amount = parseAmount(message);
                harralanderSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Harralander seed!");
            } else if (lowerMsg.contains("ranarr seed")) {
                int amount = parseAmount(message);
                ranarrSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Ranarr seed!");
            } else if (lowerMsg.contains("toadflax seed")) {
                int amount = parseAmount(message);
                toadflaxSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Toadflax seed!");
            } else if (lowerMsg.contains("irit seed")) {
                int amount = parseAmount(message);
                iritSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Irit seed!");
            } else if (lowerMsg.contains("avantoe seed")) {
                int amount = parseAmount(message);
                avantoeSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Avantoe seed!");
            } else if (lowerMsg.contains("kwuarm seed")) {
                int amount = parseAmount(message);
                kwuarmSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Kwuarm seed!");
            } else if (lowerMsg.contains("snapdragon seed")) {
                int amount = parseAmount(message);
                snapdragonSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Snapdragon seed!");
            } else if (lowerMsg.contains("cadantine seed")) {
                int amount = parseAmount(message);
                cadantineSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Cadantine seed!");
            } else if (lowerMsg.contains("lantadyme seed")) {
                int amount = parseAmount(message);
                lantadymeSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Lantadyme seed!");
            } else if (lowerMsg.contains("dwarf weed seed")) {
                int amount = parseAmount(message);
                dwarfWeedSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Dwarf weed seed!");
            } else if (lowerMsg.contains("torstol seed")) {
                int amount = parseAmount(message);
                torstolSeedCount += amount;
                script.log("INFO", "✓ Gained " + amount + "x Torstol seed!");
            }
        }
        } catch (Exception e) {
        }
    }

    private int parseAmount(String message) {
        
        
        try {
            String[] words = message.split("\\s+");
            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].toLowerCase().equals("steal")) {
                    String nextWord = words[i + 1].replaceAll("[^0-9]", "");
                    if (!nextWord.isEmpty()) {
                        return Integer.parseInt(nextWord);
                    }
                }
            }
        } catch (Exception e) {
            
        }
        return 1;
    }

    private void depositSeedsToSeedBox() {
        
        java.util.Set<Integer> SEED_IDS = java.util.Set.of(
            5291,  
            5292,  
            5293,  
            5294,  
            5295,  
            5296,  
            5297,  
            5298,  
            5299,  
            5300,  
            5301,  
            5302,  
            5303,  
            5304   
        );

        
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        script.submitTask(() -> false, 200);

        
        com.osmb.api.item.ItemGroupResult seedBoxCheck =
            script.getWidgetManager().getInventory().search(java.util.Set.of(SEED_BOX_ID));

        if (seedBoxCheck == null || !seedBoxCheck.contains(SEED_BOX_ID)) {
            script.log("WARN", "Seed box not found in inventory! Cannot deposit seeds.");
            return;
        }

        
        com.osmb.api.item.ItemGroupResult seedCheck =
            script.getWidgetManager().getInventory().search(SEED_IDS);

        if (seedCheck == null || seedCheck.isEmpty()) {
            return; 
        }

        

        
        com.osmb.api.item.ItemSearchResult seedBox = seedBoxCheck.getItem(SEED_BOX_ID);
        if (seedBox != null && seedBox.interact("Fill")) {
            script.log("INFO", "✓ Deposited seeds into seed box!");
            script.submitTask(() -> false, 500); 
        } else {
            script.log("WARN", "Failed to interact with seed box");
        }
    }

}
