package tasks;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import utils.Task;

import static main.KyyzMasterFarmer.*;

public class WalkTask extends Task {

    public WalkTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone) {
            return false;
        }

        WorldPosition currentPos = script.getWorldPosition();
        if (currentPos == null) {
            return false;
        }

        double distance = currentPos.distanceTo(farmerLocation);

        return distance > 10;
    }

    @Override
    public boolean execute() {
        task = "Walking to " + selectedLocation.getDisplayName();

        WorldPosition currentPos = script.getWorldPosition();
        if (currentPos == null) {
            script.log("WARN", "Cannot get current position");
            return false;
        }

        script.getWalker().walkTo(farmerLocation);

        script.submitHumanTask(() -> {
            WorldPosition current = script.getWorldPosition();
            return current != null && current.distanceTo(farmerLocation) < 10;
        }, script.random(30000, 35000));

        return false;
    }
}
