package tasks;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static main.KyyzMasterFarmer.*;

public class BankTask extends Task {

    private static final String[] BANK_ACTIONS = new String[]{"Bank", "Use"};

    public BankTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone) {
            return false;
        }

        // Activate ONLY if we need food (ran out during thieving)
        if (eatFood && needFood) {
            script.log("DEBUG", "BankTask activating: needFood = true");
            return true;
        }

        // Check if inventory is actually full with loot items (only for BANK_WHEN_FULL mode)
        if ("BANK_WHEN_FULL".equals(lootMode)) {
            com.osmb.api.item.ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Collections.emptySet());
            if (inv == null) {
                return false;  // Inventory not visible
            }

            // Check if inventory is full (28 slots occupied)
            if (inv.isFull()) {
                script.log("DEBUG", "BankTask activating: inventory is full");
                return true;
            }

            // Also check free slots - bank if we have 4 or fewer slots (leaves room for food)
            int freeSlots = inv.getFreeSlots();
            if (freeSlots <= 4) {
                script.log("DEBUG", "BankTask activating: only " + freeSlots + " free slots remaining");
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean execute() {
        task = "Banking";

        // Check if bank is already open
        if (!script.getWidgetManager().getBank().isVisible()) {
            script.log("INFO", "Bank not visible, opening bank...");
            openBank();
            return false;
        }

        script.log("INFO", "Bank is open - managing inventory");
        return manageBank();
    }

    private void openBank() {
        task = "Open bank";
        script.log("INFO", "Looking for nearest Bank booth...");

        // Get all Bank booth objects that are reachable and have "Bank" action
        List<RSObject> banksFound = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) return false;
            return gameObject.getName().equals("Bank booth")
                    && Arrays.stream(gameObject.getActions()).anyMatch(action ->
                    action != null && (action.equals("Bank") || action.equals("Use")))
                    && gameObject.canReach();
        });

        if (banksFound.isEmpty()) {
            script.log("WARN", "No Bank booth found! Trying Bank chest...");

            // Fallback to Bank chest
            banksFound = script.getObjectManager().getObjects(gameObject -> {
                if (gameObject.getName() == null || gameObject.getActions() == null) return false;
                return gameObject.getName().equals("Bank chest")
                        && Arrays.stream(gameObject.getActions()).anyMatch(action ->
                        action != null && (action.equals("Bank") || action.equals("Use")))
                        && gameObject.canReach();
            });

            if (banksFound.isEmpty()) {
                script.log("WARN", "No bank objects found!");
                return;
            }
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (bank == null) {
            script.log("WARN", "Failed to get closest bank");
            return;
        }

        double distance = bank.getWorldPosition().distanceTo(script.getWorldPosition());
        script.log("INFO", "Found '" + bank.getName() + "' at distance: " + distance);

        // Interact with bank - this handles pathfinding automatically
        script.log("INFO", "Interacting with '" + bank.getName() + "'...");
        if (!bank.interact("Bank")) {
            script.log("WARN", "Failed to interact with bank");
            return;
        }

        // Wait for bank to open (includes time for walking)
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        task = "Wait for bank open";
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
        task = "Deposit items";
        script.log("INFO", "Depositing all items...");

        // If using seed box, empty it first before depositing inventory
        if (useSeedBox) {
            emptySeedBox();
        }

        // Deposit everything
        if (!script.getWidgetManager().getBank().depositAll(Collections.emptySet())) {
            script.log("WARN", "Deposit all action failed");
            return false;
        }

        // Only withdraw food if eating is enabled and foodItemId is valid
        if (eatFood && foodItemId > 0) {
            task = "Withdraw food";
            script.log("INFO", "Checking if " + foodName + " is available in bank...");

            // Check if we have food in bank first
            com.osmb.api.item.ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(java.util.Set.of(foodItemId));
            if (bankSnapshot == null || !bankSnapshot.contains(foodItemId)) {
                script.log("WARN", "No " + foodName + " found in bank! Stopping script.");
                script.stop();
                return false;
            }

            int availableFood = bankSnapshot.getAmount(foodItemId);
            script.log("INFO", "Found " + availableFood + " " + foodName + " in bank. Withdrawing " + foodAmount + "...");

            // Withdraw food items using ItemID (Bank API searches all tabs automatically)
            boolean withdrawSuccess = script.getWidgetManager().getBank().withdraw(foodItemId, foodAmount);
            if (!withdrawSuccess) {
                script.log("WARN", "Bank.withdraw() returned false - trying again...");
                script.submitHumanTask(() -> false, script.random(1000, 2000));
                withdrawSuccess = script.getWidgetManager().getBank().withdraw(foodItemId, foodAmount);
                if (!withdrawSuccess) {
                    script.log("WARN", "Failed to withdraw " + foodName + " after retry. Stopping.");
                    script.stop();
                    return false;
                }
            }

            // Reset the needFood flag NOW before walking back
            needFood = false;
            script.log("INFO", "Food restocked successfully!");
        } else {
            script.log("INFO", "No food mode enabled - skipping food withdrawal");
        }

        // If using seed box, withdraw it from bank
        if (useSeedBox) {
            withdrawSeedBox();
        }

        // Close bank
        task = "Close bank";
        script.log("INFO", "Closing bank...");
        script.getWidgetManager().getBank().close();
        script.submitHumanTask(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));

        // Reopen inventory tab after bank closes
        task = "Reopen inventory";
        script.log("INFO", "Reopening inventory tab...");
        script.getWidgetManager().getTabManager().openTab(com.osmb.api.ui.tabs.Tab.Type.INVENTORY);

        // Walk back to Draynor Master Farmer location
        task = "Return to Draynor";
        script.log("INFO", "Walking back to Draynor Master Farmer location: " + DRAYNOR_FARMER_LOCATION);
        script.getWalker().walkTo(DRAYNOR_FARMER_LOCATION);
        script.submitHumanTask(() -> {
            WorldPosition current = script.getWorldPosition();
            return current != null && current.distanceTo(DRAYNOR_FARMER_LOCATION) < 5;
        }, script.random(20000, 25000));

        return false;  // Return false so task doesn't keep executing
    }

    private void emptySeedBox() {
        task = "Empty seed box";
        script.log("INFO", "Emptying seed box into bank...");

        // Open inventory tab to access seed box
        script.getWidgetManager().getTabManager().openTab(com.osmb.api.ui.tabs.Tab.Type.INVENTORY);
        script.submitTask(() -> false, 300);

        // Check if we have a seed box in inventory
        com.osmb.api.item.ItemGroupResult seedBoxCheck =
            script.getWidgetManager().getInventory().search(java.util.Set.of(SEED_BOX_ID));

        if (seedBoxCheck == null || !seedBoxCheck.contains(SEED_BOX_ID)) {
            script.log("WARN", "Seed box not found in inventory! Skipping empty.");
            return;
        }

        // Get the seed box item and interact with it (Empty action)
        com.osmb.api.item.ItemSearchResult seedBox = seedBoxCheck.getItem(SEED_BOX_ID);
        if (seedBox != null && seedBox.interact("Empty")) {
            script.log("INFO", "✓ Emptied seed box into bank!");
            script.submitTask(() -> false, 800); // Wait for seeds to be deposited
        } else {
            script.log("WARN", "Failed to empty seed box - trying alternative method");
            // Try "Check" action which might open interface
            if (seedBox != null && seedBox.interact("Check")) {
                script.log("DEBUG", "Opened seed box interface");
                script.submitTask(() -> false, 500);
            }
        }
    }

    private void withdrawSeedBox() {
        task = "Withdraw seed box";
        script.log("INFO", "Withdrawing seed box from bank...");

        // Check if seed box is in bank
        com.osmb.api.item.ItemGroupResult bankSnapshot =
            script.getWidgetManager().getBank().search(java.util.Set.of(SEED_BOX_ID));

        if (bankSnapshot == null || !bankSnapshot.contains(SEED_BOX_ID)) {
            script.log("WARN", "Seed box not found in bank! Make sure to have one before starting.");
            return;
        }

        // Withdraw 1 seed box
        boolean withdrawSuccess = script.getWidgetManager().getBank().withdraw(SEED_BOX_ID, 1);
        if (withdrawSuccess) {
            script.log("INFO", "✓ Seed box withdrawn!");
            script.submitTask(() -> false, 500);
        } else {
            script.log("WARN", "Failed to withdraw seed box from bank");
        }
    }
}
