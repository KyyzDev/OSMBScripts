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
    private int consecutiveFailures = 0;
    private static final int MAX_FAILURES_BEFORE_REPOSITION = 12;

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
                return false;
            }
        }

        com.osmb.api.item.ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Collections.emptySet());
        if (inv != null && inv.isFull()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean execute() {
        task = "Pickpocketing " + targetNPC;

        UIResultList<WorldPosition> npcPositions = script.getWidgetManager().getMinimap().getNPCPositions();

        if (npcPositions.isNotVisible() || npcPositions.isNotFound()) {
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

            Polygon cubeResized = poly.convexHull();
            if (cubeResized == null) {
                cubeResized = poly;
            }

            List<Point> cyanPixels = script.getPixelAnalyzer().findPixelsOnGameScreen(cubeResized, CYAN_HIGHLIGHT);

            if (cyanPixels == null || cyanPixels.size() < 3) {
                continue;
            }

            int[] xPoints = new int[cyanPixels.size()];
            int[] yPoints = new int[cyanPixels.size()];
            for (int i = 0; i < cyanPixels.size(); i++) {
                xPoints[i] = cyanPixels.get(i).x;
                yPoints[i] = cyanPixels.get(i).y;
            }
            Polygon cyanPolygon = new Polygon(xPoints, yPoints).convexHull();

            if (cyanPolygon == null) {
                continue;
            }

            boolean tapped = script.getFinger().tapGameScreen(cyanPolygon, menuEntries -> {
                for (int i = 0; i < menuEntries.size(); i++) {
                    String action = menuEntries.get(i).getAction();
                    String entityName = menuEntries.get(i).getEntityName();

                    if (action != null && entityName != null) {
                        String actionLower = action.toLowerCase();
                        String entityLower = entityName.toLowerCase();
                        String targetLower = targetNPC.toLowerCase();

                        if (actionLower.contains("pickpocket") && entityLower.equals(targetLower)) {
                            return menuEntries.get(i);
                        }
                    }
                }
                return null;
            });

            if (tapped) {
                pickpocketCount++;
                consecutiveFailures = 0;
                lastSuccessfulAction = System.currentTimeMillis();
                hasAttemptedPickpocket = true;

                script.submitTask(() -> false, script.random(80, 150));
                trackPickpocketResultFromChat();

                if (useSeedBox) {
                    seedBoxFillCounter++;

                    if (seedBoxFillCounter >= seedBoxFillThreshold) {
                        boolean hadSeeds = depositSeedsToSeedBox();

                        if (hadSeeds) {
                            seedBoxFillCounter = 0;
                            seedBoxFillThreshold = 7 + (int)(Math.random() * 9);
                        } else {
                            seedBoxFillCounter = 0;
                        }
                    }
                }

                script.pollFramesUntil(() -> !script.getPixelAnalyzer().isPlayerAnimating(0.3), 1500);

                if (processedMessages.size() > 200) {
                    processedMessages.clear();
                }

                return true;
            }
        }

        consecutiveFailures++;

        if (consecutiveFailures >= MAX_FAILURES_BEFORE_REPOSITION) {
            consecutiveFailures = 0;

            WorldPosition currentPos = script.getWorldPosition();
            if (currentPos != null && currentPos.distanceTo(centerTile) > 2) {
                script.getWalker().walkTo(centerTile);
                script.submitHumanTask(() -> {
                    WorldPosition current = script.getWorldPosition();
                    return current != null && current.distanceTo(centerTile) < 2;
                }, script.random(3000, 5000));
            }
        }

        return false;
    }

    private void trackPickpocketResultFromChat() {
        try {
            com.osmb.api.utils.UIResultList<String> chatMessages = script.getWidgetManager().getChatbox().getText();

            if (chatMessages == null || !chatMessages.isFound() || chatMessages.isEmpty()) {
                return;
            }

            for (String message : chatMessages) {
                if (message == null) continue;
                if (processedMessages.contains(message)) continue;

                String lowerMsg = message.toLowerCase();

                if (lowerMsg.contains("you steal") || lowerMsg.contains("you pick the") || lowerMsg.contains("you pickpocket")) {
                    successfulPickpockets++;
                    totalThievingXP += MASTER_FARMER_XP;
                    processedMessages.add(message);
                    chatDetected = true;
                } else if (lowerMsg.contains("you fail") || lowerMsg.contains("failed to pick") || lowerMsg.contains("couldn't get")) {
                    failedPickpockets++;
                    processedMessages.add(message);
                    chatDetected = true;
                }

                if (!lowerMsg.contains("you steal")) continue;

                if (lowerMsg.contains("guam seed")) {
                    guamSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("marrentill seed")) {
                    marrentillSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("tarromin seed")) {
                    tarrominSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("harralander seed")) {
                    harralanderSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("ranarr seed")) {
                    ranarrSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("toadflax seed")) {
                    toadflaxSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("irit seed")) {
                    iritSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("avantoe seed")) {
                    avantoeSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("kwuarm seed")) {
                    kwuarmSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("snapdragon seed")) {
                    snapdragonSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("cadantine seed")) {
                    cadantineSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("lantadyme seed")) {
                    lantadymeSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("dwarf weed seed")) {
                    dwarfWeedSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("torstol seed")) {
                    torstolSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("limpwurt seed")) {
                    limpwurtSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("watermelon seed")) {
                    watermelonSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("snape grass seed")) {
                    snapeGrassSeedCount += parseAmount(message);
                } else if (lowerMsg.contains("seaweed spore")) {
                    seaweedSporeCount += parseAmount(message);
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
                    if (!nextWord.isEmpty()) return Integer.parseInt(nextWord);
                }
            }
        } catch (Exception e) {}
        return 1;
    }

    private boolean depositSeedsToSeedBox() {
        java.util.Set<Integer> SEED_IDS = java.util.Set.of(
            5291, 5292, 5293, 5294, 5295, 5296, 5297, 5298, 5299, 5300, 5301, 5302, 5303, 5304,
            5100, 5321, 22879, 21490
        );

        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        script.submitTask(() -> false, 200);

        com.osmb.api.item.ItemGroupResult seedBoxCheck = script.getWidgetManager().getInventory().search(SEED_BOX_IDS);
        if (seedBoxCheck == null || seedBoxCheck.isEmpty()) {
            return false;
        }

        com.osmb.api.item.ItemGroupResult seedCheck = script.getWidgetManager().getInventory().search(SEED_IDS);
        if (seedCheck == null || seedCheck.isEmpty()) return false;

        com.osmb.api.item.ItemSearchResult seedBox = seedBoxCheck.getItem(SEED_BOX_IDS);
        if (seedBox == null) {
            return false;
        }

        if (seedBox.getId() == SEED_BOX_OPEN) {
            script.submitTask(() -> false, 200);
            return true;
        }

        if (seedBox.interact("Fill")) {
            script.submitTask(() -> false, 500);
            return true;
        } else {
            return false;
        }
    }
}
