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

    // Cyan highlight colors used by OSRS entity highlighting
    // -14155777 = 0xFF27FFFF (bright cyan)
    // -2171877 = 0xFFDEDC1B (yellow-ish highlight variation)
    // 0xFF05F8F8 = original cyan setting
    private static final SearchablePixel[] CYAN_HIGHLIGHT = new SearchablePixel[]{
            new SearchablePixel(-14155777, new SingleThresholdComparator(15), ColorModel.RGB),
            new SearchablePixel(-2171877, new SingleThresholdComparator(15), ColorModel.RGB),
            new SearchablePixel(0xFF05F8F8, new SingleThresholdComparator(15), ColorModel.RGB),
    };

    public ThieveTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone) {
            return false;
        }

        // CRITICAL: Do NOT pickpocket if HP is low and we need to eat!
        if (eatFood) {
            Integer currentHp = script.getWidgetManager().getMinimapOrbs().getHitpoints();
            if (currentHp != null && currentHp <= hpThreshold) {
                script.log("DEBUG", "ThieveTask: HP is " + currentHp + " (threshold: " + hpThreshold + ") - NOT pickpocketing, letting EatTask handle it!");
                return false;  // Let EatTask activate instead
            }
        }

        return true;
    }

    @Override
    public boolean execute() {
        task = "Pickpocketing " + targetNPC;

        // Get NPC positions from minimap
        UIResultList<WorldPosition> npcPositions = script.getWidgetManager().getMinimap().getNPCPositions();

        if (npcPositions.isNotVisible() || npcPositions.isNotFound()) {
            script.log("WARN", "No NPCs found on minimap");
            return false;
        }

        script.log("DEBUG", "Found " + npcPositions.size() + " NPC positions on minimap");

        // Try each NPC position
        for (WorldPosition position : npcPositions) {
            LocalPosition localPosition = position.toLocalPosition(script);
            if (localPosition == null) {
                continue;
            }

            // Get tile cube for this NPC position
            Polygon poly = script.getSceneProjector().getTileCube(
                localPosition.getX(),
                localPosition.getY(),
                localPosition.getPlane(),
                150
            );

            if (poly == null) {
                continue;
            }

            // Resize the polygon for better clicking (smaller = more precise)
            Polygon resized = poly.getResized(0.8);
            Polygon cubeResized = (resized != null) ? resized.convexHull() : null;
            if (cubeResized == null) {
                cubeResized = poly;
            }

            // FIRST: Check if this NPC position has cyan highlight pixels
            List<Point> cyanPixels = script.getPixelAnalyzer().findPixelsOnGameScreen(cubeResized, CYAN_HIGHLIGHT);

            if (cyanPixels == null || cyanPixels.size() < 5) {
                script.log("DEBUG", "NPC at " + position + " has no cyan highlight (" +
                    (cyanPixels != null ? cyanPixels.size() : 0) + " pixels) - skipping");
                continue;
            }

            script.log("INFO", "Found cyan highlighted NPC at " + position + " with " + cyanPixels.size() + " cyan pixels - attempting pickpocket");

            // SECOND: Try to tap with "Pickpocket" action
            // Since we already filtered for cyan highlight, we know it's the right NPC
            boolean tapped = script.getFinger().tapGameScreen(cubeResized, menuEntries -> {
                script.log("DEBUG", "Menu has " + menuEntries.size() + " entries:");

                for (int i = 0; i < menuEntries.size(); i++) {
                    String action = menuEntries.get(i).getAction();
                    String entityName = menuEntries.get(i).getEntityName();

                    script.log("DEBUG", "  [" + i + "] action='" + action + "', entity='" + entityName + "'");

                    // Check for "Pickpocket" action (case insensitive) on Master Farmer
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

                // Wait a moment for chat message to appear
                script.submitTask(() -> false, 300);

                // Check chat for "You steal" messages and track success/failure
                trackPickpocketResultFromChat();

                // If using seed box, deposit any seeds we just picked up
                if (useSeedBox) {
                    depositSeedsToSeedBox();
                }

                // Wait for animation/stun to finish
                script.pollFramesUntil(() ->
                    !script.getPixelAnalyzer().isPlayerAnimating(0.3),
                    5000
                );

                return true;
            } else {
                script.log("WARN", "Failed to tap at position " + position + " - tapGameScreen returned false");
            }
        }

        script.log("WARN", "No " + targetNPC + " found nearby");
        return false;
    }

    private void trackPickpocketResultFromChat() {
        // Read chat messages - looking for "You steal" (success) or "You fail" (failed)
        com.osmb.api.utils.UIResultList<String> chatMessages = script.getWidgetManager().getChatbox().getText();

        if (chatMessages == null || !chatMessages.isFound() || chatMessages.isEmpty()) {
            script.log("DEBUG", "No chat messages found");
            return;
        }

        // Check recent chat messages
        for (String message : chatMessages) {
            if (message == null) {
                continue;
            }

            script.log("DEBUG", "Chat message: " + message);

            String lowerMsg = message.toLowerCase();

            // Track successful pickpockets
            if (lowerMsg.contains("you steal")) {
                successfulPickpockets++;
                script.log("DEBUG", "✓ Successful pickpocket! Total: " + successfulPickpockets);
            }
            // Track failed pickpockets
            else if (lowerMsg.contains("you fail") || lowerMsg.contains("failed to pick")) {
                failedPickpockets++;
                script.log("DEBUG", "✗ Failed pickpocket! Total: " + failedPickpockets);
            }

            // Continue checking for seeds only if it's a successful "You steal" message
            if (!lowerMsg.contains("you steal")) {
                continue;
            }

            // Parse the message - format is typically "You steal x [item name]."

            // Check each seed type
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
    }

    private int parseAmount(String message) {
        // Try to extract number from message like "You steal 3 Guam seeds."
        // If no number found, assume 1
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
            // If parsing fails, default to 1
        }
        return 1;
    }

    private void depositSeedsToSeedBox() {
        // All herb seed IDs that Master Farmer drops
        java.util.Set<Integer> SEED_IDS = java.util.Set.of(
            5291,  // Guam seed
            5292,  // Marrentill seed
            5293,  // Tarromin seed
            5294,  // Harralander seed
            5295,  // Ranarr seed
            5296,  // Toadflax seed
            5297,  // Irit seed
            5298,  // Avantoe seed
            5299,  // Kwuarm seed
            5300,  // Snapdragon seed
            5301,  // Cadantine seed
            5302,  // Lantadyme seed
            5303,  // Dwarf weed seed
            5304   // Torstol seed
        );

        // Open inventory to see seeds
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        script.submitTask(() -> false, 200);

        // Check if we have a seed box in inventory
        com.osmb.api.item.ItemGroupResult seedBoxCheck =
            script.getWidgetManager().getInventory().search(java.util.Set.of(SEED_BOX_ID));

        if (seedBoxCheck == null || !seedBoxCheck.contains(SEED_BOX_ID)) {
            script.log("WARN", "Seed box not found in inventory! Cannot deposit seeds.");
            return;
        }

        // Check inventory for any seeds
        com.osmb.api.item.ItemGroupResult seedCheck =
            script.getWidgetManager().getInventory().search(SEED_IDS);

        if (seedCheck == null || seedCheck.isEmpty()) {
            return; // No seeds to deposit
        }

        script.log("DEBUG", "Seeds found in inventory - depositing to seed box...");

        // Get the seed box item and interact with it (Fill action)
        com.osmb.api.item.ItemSearchResult seedBox = seedBoxCheck.getItem(SEED_BOX_ID);
        if (seedBox != null && seedBox.interact("Fill")) {
            script.log("INFO", "✓ Deposited seeds into seed box!");
            script.submitTask(() -> false, 500); // Wait for seeds to be deposited
        } else {
            script.log("WARN", "Failed to interact with seed box");
        }
    }

}
