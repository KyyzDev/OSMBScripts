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
            script.log("DEBUG", "BankTask activating: needFood = true");
            return true;
        }

        
        if ("BANK_WHEN_FULL".equals(lootMode)) {
            com.osmb.api.item.ItemGroupResult inv = script.getWidgetManager().getInventory().search(java.util.Collections.emptySet());
            if (inv == null) {
                return false;  
            }

            
            if (inv.isFull()) {
                script.log("DEBUG", "BankTask activating: inventory is full");
                return true;
            }

            
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


        if (!script.getWidgetManager().getBank().isVisible()) {
            bankOpenAttempts++;

            if (bankOpenAttempts >= MAX_BANK_ATTEMPTS) {
                script.log("WARN", "========================================");
                script.log("WARN", "  FAILED TO OPEN BANK!");
                script.log("WARN", "  Possible bank PIN issue");
                script.log("WARN", "  Make sure bank PIN is configured in OSMB");
                script.log("WARN", "  Logging out safely...");
                script.log("WARN", "========================================");

                script.getWidgetManager().getLogoutTab().logout();
                script.submitTask(() -> false, 2000);
                script.stop();
                return false;
            }

            script.log("INFO", "Bank not visible, opening bank... (Attempt " + bankOpenAttempts + "/" + MAX_BANK_ATTEMPTS + ")");
            openBank();
            return false;
        }

        bankOpenAttempts = 0;
        script.log("INFO", "Bank is open - managing inventory");
        return manageBank();
    }

    private void openBank() {
        task = "Open bank";
        script.log("INFO", "Looking for nearest Bank booth...");

        
        List<RSObject> banksFound = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) return false;
            return gameObject.getName().equals("Bank booth")
                    && Arrays.stream(gameObject.getActions()).anyMatch(action ->
                    action != null && (action.equals("Bank") || action.equals("Use")))
                    && gameObject.canReach();
        });

        if (banksFound.isEmpty()) {
            script.log("WARN", "No Bank booth found! Trying Bank chest...");

            
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

        
        script.log("INFO", "Interacting with '" + bank.getName() + "'...");
        if (!bank.interact("Bank")) {
            script.log("WARN", "Failed to interact with bank");
            return;
        }

        
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

        
        if (useSeedBox) {
            emptySeedBox();
        }


        if (!script.getWidgetManager().getBank().depositAll(Collections.emptySet())) {
            script.log("WARN", "Deposit all action failed");
            return false;
        }


        script.submitTask(() -> false, 500);


        task = "Check bank space";
        com.osmb.api.item.ItemGroupResult bankCheck = script.getWidgetManager().getBank().search(Collections.emptySet());
        if (bankCheck != null) {
            int bankFreeSlots = bankCheck.getFreeSlots();
            script.log("INFO", "Bank has " + bankFreeSlots + " free slots");

            if (bankFreeSlots == 0) {
                script.log("WARN", "========================================");
                script.log("WARN", "  BANK IS FULL!");
                script.log("WARN", "  0 slots remaining");
                script.log("WARN", "  Logging out safely...");
                script.log("WARN", "========================================");

                script.getWidgetManager().getBank().close();
                script.submitTask(() -> false, 1000);
                script.getWidgetManager().getLogoutTab().logout();
                script.submitTask(() -> false, 2000);
                script.stop();
                return false;
            }
        }


        if (eatFood && foodItemId > 0) {
            task = "Withdraw food";
            script.log("INFO", "Checking if " + foodName + " is available in bank...");

            
            com.osmb.api.item.ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(java.util.Set.of(foodItemId));
            if (bankSnapshot == null || !bankSnapshot.contains(foodItemId)) {
                script.log("WARN", "No " + foodName + " found in bank! Stopping script.");
                script.stop();
                return false;
            }

            int availableFood = bankSnapshot.getAmount(foodItemId);
            script.log("INFO", "Found " + availableFood + " " + foodName + " in bank. Withdrawing " + foodAmount + "...");

            
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

            
            needFood = false;
            script.log("INFO", "Food restocked successfully!");
        } else {
            script.log("INFO", "No food mode enabled - skipping food withdrawal");
        }

        
        if (useSeedBox) {
            withdrawSeedBox();
        }

        
        task = "Close bank";
        script.log("INFO", "Closing bank...");
        script.getWidgetManager().getBank().close();
        script.submitHumanTask(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));

        
        task = "Reopen inventory";
        script.log("INFO", "Reopening inventory tab...");
        script.getWidgetManager().getTabManager().openTab(com.osmb.api.ui.tabs.Tab.Type.INVENTORY);


        task = "Return to Draynor";
        script.log("INFO", "Walking back to Draynor Master Farmer location");
        script.getWalker().walkTo(DRAYNOR_FARMER_LOCATION);
        script.submitHumanTask(() -> {
            WorldPosition current = script.getWorldPosition();
            return current != null && current.distanceTo(DRAYNOR_FARMER_LOCATION) < 5;
        }, script.random(20000, 25000));

        return false;  
    }

    private void emptySeedBox() {
        task = "Empty seed box";
        script.log("INFO", "Emptying seed box into bank...");

        
        script.getWidgetManager().getTabManager().openTab(com.osmb.api.ui.tabs.Tab.Type.INVENTORY);
        script.submitTask(() -> false, 300);

        
        com.osmb.api.item.ItemGroupResult seedBoxCheck =
            script.getWidgetManager().getInventory().search(java.util.Set.of(SEED_BOX_ID));

        if (seedBoxCheck == null || !seedBoxCheck.contains(SEED_BOX_ID)) {
            script.log("WARN", "Seed box not found in inventory! Skipping empty.");
            return;
        }

        
        com.osmb.api.item.ItemSearchResult seedBox = seedBoxCheck.getItem(SEED_BOX_ID);
        if (seedBox != null && seedBox.interact("Empty")) {
            script.log("INFO", "✓ Emptied seed box into bank!");
            script.submitTask(() -> false, 800); 
        } else {
            script.log("WARN", "Failed to empty seed box - trying alternative method");
            
            if (seedBox != null && seedBox.interact("Check")) {
                script.log("DEBUG", "Opened seed box interface");
                script.submitTask(() -> false, 500);
            }
        }
    }

    private void withdrawSeedBox() {
        task = "Withdraw seed box";
        script.log("INFO", "Withdrawing seed box from bank...");

        
        com.osmb.api.item.ItemGroupResult bankSnapshot =
            script.getWidgetManager().getBank().search(java.util.Set.of(SEED_BOX_ID));

        if (bankSnapshot == null || !bankSnapshot.contains(SEED_BOX_ID)) {
            script.log("WARN", "Seed box not found in bank! Make sure to have one before starting.");
            return;
        }

        
        boolean withdrawSuccess = script.getWidgetManager().getBank().withdraw(SEED_BOX_ID, 1);
        if (withdrawSuccess) {
            script.log("INFO", "✓ Seed box withdrawn!");
            script.submitTask(() -> false, 500);
        } else {
            script.log("WARN", "Failed to withdraw seed box from bank");
        }
    }
}
