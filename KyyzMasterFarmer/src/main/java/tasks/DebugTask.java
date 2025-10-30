package tasks;

import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.List;

import static main.KyyzMasterFarmer.*;

public class DebugTask extends Task {

    private long lastDebug = 0;

    public DebugTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        // Only run every 5 seconds
        return setupDone && System.currentTimeMillis() - lastDebug > 5000;
    }

    @Override
    public boolean execute() {
        lastDebug = System.currentTimeMillis();

        script.log("DEBUG", "=== Scanning for NPCs ===");

        // Get ALL objects nearby
        List<RSObject> allObjects = script.getObjectManager().getObjects(obj ->
            obj.getName() != null && obj.canReach()
        );

        script.log("DEBUG", "Found " + allObjects.size() + " reachable objects");

        // Log first 10 objects with their names and actions
        int count = 0;
        for (RSObject obj : allObjects) {
            if (count >= 10) break;

            String actions = "No actions";
            if (obj.getActions() != null) {
                actions = String.join(", ", obj.getActions());
            }

            script.log("DEBUG", "Object: '" + obj.getName() + "' | Actions: [" + actions + "]");
            count++;
        }

        // Specifically look for anything with "Farmer" in the name
        List<RSObject> farmers = script.getObjectManager().getObjects(obj ->
            obj.getName() != null &&
            obj.getName().toLowerCase().contains("farmer")
        );

        script.log("DEBUG", "=== Objects with 'Farmer' in name: " + farmers.size() + " ===");
        for (RSObject farmer : farmers) {
            String actions = farmer.getActions() != null ? String.join(", ", farmer.getActions()) : "None";
            script.log("DEBUG", "FARMER: '" + farmer.getName() + "' | Actions: [" + actions + "] | Reachable: " + farmer.canReach());
        }

        return false; // Don't actually do anything
    }
}
