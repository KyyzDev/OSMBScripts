package tasks;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static main.KyyzMasterFarmer.*;

public class BankTask extends Task {

    private int bankOpenAttempts = 0;
    private static final int MAX_BANK_ATTEMPTS = 3;

    public BankTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone) {
            return false;
        }

        if (eatFood && needFood) {
            return true;
        }

        if (useSeedBox && needSeedBox) {
            return true;
        }

        if ("BANK_WHEN_FULL".equals(lootMode)) {
            com.osmb.api.item.ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Collections.emptySet());
            if (inv == null) {
                return false;
            }

            if (inv.isFull() || inv.getFreeSlots() <= 1) {
                return true;
            }
        }

        bankOpenAttempts = 0;
        return false;
    }

    @Override
    public boolean execute() {
        task = "Banking";

        if ("DROP_ALL".equals(lootMode) && bankOpenAttempts == 0) {
            task = "Dropping items";

            script.getWidgetManager().getTabManager().openTab(com.osmb.api.ui.tabs.Tab.Type.INVENTORY);
            script.submitTask(() -> false, 300);

            com.osmb.api.item.ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Collections.emptySet());
            if (inv != null && !inv.isEmpty()) {
                java.util.List<com.osmb.api.item.ItemSearchResult> allItems = inv.getRecognisedItems();
                java.util.Set<Integer> itemsToKeep = new java.util.HashSet<>();
                if (foodItemId > 0) itemsToKeep.add(foodItemId);

                java.util.Set<Integer> allItemsToDrop = new java.util.HashSet<>();
                for (com.osmb.api.item.ItemSearchResult item : allItems) {
                    int id = item.getId();
                    if (!itemsToKeep.contains(id)) allItemsToDrop.add(id);
                }

                if (!allItemsToDrop.isEmpty()) {
                    for (int attempt = 0; attempt < 3; attempt++) {
                        script.getWidgetManager().getInventory().dropItems(allItemsToDrop);
                        script.submitTask(() -> false, 600);
                        com.osmb.api.item.ItemGroupResult afterDrop = script.getWidgetManager().getInventory().search(allItemsToDrop);
                        if (afterDrop == null || afterDrop.isEmpty()) break;
                    }
                }
            }
        }

        if (!script.getWidgetManager().getBank().isVisible()) {
            bankOpenAttempts++;

            if (bankOpenAttempts >= MAX_BANK_ATTEMPTS) {
                script.log("WARN", "Failed to open bank - possible PIN issue");
                script.getWidgetManager().getLogoutTab().logout();
                script.submitTask(() -> false, 2000);
                script.stop();
                return false;
            }

            openBank();
            return false;
        }

        bankOpenAttempts = 0;
        return manageBank();
    }

    private void openBank() {
        task = "Open bank";

        List<RSObject> banksFound = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) return false;
            return gameObject.getName().equals("Bank booth")
                    && Arrays.stream(gameObject.getActions()).anyMatch(action ->
                    action != null && (action.equals("Bank") || action.equals("Use")))
                    && gameObject.canReach();
        });

        if (banksFound.isEmpty()) {
            banksFound = script.getObjectManager().getObjects(gameObject -> {
                if (gameObject.getName() == null || gameObject.getActions() == null) return false;
                return gameObject.getName().equals("Bank chest")
                        && Arrays.stream(gameObject.getActions()).anyMatch(action ->
                        action != null && (action.equals("Bank") || action.equals("Use")))
                        && gameObject.canReach();
            });

            if (banksFound.isEmpty()) {
                return;
            }
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (bank == null) {
            return;
        }

        if (!bank.interact("Bank")) {
            return;
        }

        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        task = "Wait for bank";
        script.submitHumanTask(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!Objects.equals(current, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(current);
            }

            return script.getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, script.random(14000, 16000));
    }

    private boolean manageBank() {
        com.osmb.api.utils.UIResultList<String> chatMessages = script.getWidgetManager().getChatbox().getText();
        if (chatMessages != null && chatMessages.isFound() && !chatMessages.isEmpty()) {
            for (String message : chatMessages) {
                if (message == null) continue;
                if (message.toLowerCase().contains("your bank is full")) {
                    script.log("WARN", "Bank is full - stopping");
                    script.getWidgetManager().getBank().close();
                    script.submitTask(() -> false, 1000);
                    script.stop();
                    return false;
                }
            }
        }

        if ("BANK_WHEN_FULL".equals(lootMode)) {
            task = "Deposit items";

            if (useSeedBox) {
                emptySeedBox();
            }

            java.util.Set<Integer> keepItems = new java.util.HashSet<>();
            if (useSeedBox) {
                keepItems.addAll(SEED_BOX_IDS);
            }
            if (eatFood && foodItemId > 0) {
                keepItems.addAll(getFoodIds(foodItemId));
            }

            for (int attempt = 1; attempt <= 3; attempt++) {
                if (!script.getWidgetManager().getBank().depositAll(keepItems)) {
                    script.submitTask(() -> false, 500);
                    continue;
                }

                script.submitTask(() -> false, 600);

                com.osmb.api.item.ItemGroupResult remainingItems = script.getWidgetManager().getInventory().search(java.util.Collections.emptySet());
                if (remainingItems == null || remainingItems.isEmpty()) {
                    break;
                }

                java.util.List<com.osmb.api.item.ItemSearchResult> items = remainingItems.getRecognisedItems();
                boolean onlyKeepItems = true;
                for (com.osmb.api.item.ItemSearchResult item : items) {
                    if (!keepItems.contains(item.getId())) {
                        onlyKeepItems = false;
                        break;
                    }
                }

                if (onlyKeepItems) {
                    break;
                }
            }

            if (useSeedBox) {
                script.submitTask(() -> false, 300);
                com.osmb.api.item.ItemGroupResult invCheck = script.getWidgetManager().getInventory().search(SEED_BOX_IDS);
                if (invCheck == null || invCheck.isEmpty()) {
                    needSeedBox = true;
                }
            }

            script.submitTask(() -> false, 500);
        }

        if (eatFood && foodItemId > 0) {
            task = "Withdraw food";
            java.util.Set<Integer> allFoodIds = getFoodIds(foodItemId);

            com.osmb.api.item.ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(allFoodIds);
            if (bankSnapshot == null || bankSnapshot.isEmpty()) {
                script.log("WARN", "Out of food - logging out");
                script.getWidgetManager().getBank().close();
                script.submitTask(() -> false, 500);
                script.getWidgetManager().getLogoutTab().logout();
                script.submitTask(() -> false, 2000);
                script.stop();
                return false;
            }

            com.osmb.api.item.ItemSearchResult foodInBank = bankSnapshot.getItem(allFoodIds);
            int actualFoodId = (foodInBank != null) ? foodInBank.getId() : foodItemId;

            com.osmb.api.item.ItemGroupResult invCheck = script.getWidgetManager().getInventory().search(allFoodIds);
            int currentFood = (invCheck != null && !invCheck.isEmpty()) ? invCheck.getAmount(allFoodIds) : 0;

            if (currentFood < foodAmount) {
                int toWithdraw = foodAmount - currentFood;
                boolean withdrawSuccess = script.getWidgetManager().getBank().withdraw(actualFoodId, toWithdraw);
                if (!withdrawSuccess) {
                    script.log("WARN", "Failed to withdraw food - logging out");
                    script.getWidgetManager().getBank().close();
                    script.submitTask(() -> false, 500);
                    script.getWidgetManager().getLogoutTab().logout();
                    script.submitTask(() -> false, 2000);
                    script.stop();
                    return false;
                }
                script.submitTask(() -> false, 300);
            }

            needFood = false;
        }

        if (useSeedBox && needSeedBox) {
            withdrawSeedBox();
        }

        task = "Close bank";
        script.getWidgetManager().getBank().close();
        script.submitHumanTask(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));

        script.getWidgetManager().getTabManager().openTab(com.osmb.api.ui.tabs.Tab.Type.INVENTORY);

        task = "Return to " + selectedLocation.getDisplayName();
        script.getWalker().walkTo(farmerLocation);
        script.submitHumanTask(() -> {
            WorldPosition current = script.getWorldPosition();
            return current != null && current.distanceTo(farmerLocation) < 5;
        }, script.random(20000, 25000));

        return false;
    }

    private void emptySeedBox() {
        task = "Empty seed box";

        script.getWidgetManager().getTabManager().openTab(com.osmb.api.ui.tabs.Tab.Type.INVENTORY);
        script.submitTask(() -> false, 300);

        com.osmb.api.item.ItemGroupResult seedBoxCheck = script.getWidgetManager().getInventory().search(SEED_BOX_IDS);
        if (seedBoxCheck == null || seedBoxCheck.isEmpty()) {
            return;
        }

        com.osmb.api.item.ItemSearchResult seedBox = seedBoxCheck.getItem(SEED_BOX_IDS);
        if (seedBox != null && seedBox.interact("Empty")) {
            script.submitTask(() -> false, 800);
        } else if (seedBox != null) {
            seedBox.interact("Check");
            script.submitTask(() -> false, 500);
        }
    }

    private void withdrawSeedBox() {
        task = "Withdraw seed box";

        com.osmb.api.item.ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(SEED_BOX_IDS);
        if (bankSnapshot == null || bankSnapshot.isEmpty()) {
            return;
        }

        com.osmb.api.item.ItemSearchResult seedBoxInBank = bankSnapshot.getItem(SEED_BOX_IDS);
        if (seedBoxInBank == null) {
            return;
        }

        boolean withdrawSuccess = script.getWidgetManager().getBank().withdraw(seedBoxInBank.getId(), 1);
        if (withdrawSuccess) {
            needSeedBox = false;
            script.submitTask(() -> false, 500);
        }
    }

}
