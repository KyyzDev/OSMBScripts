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

        double distance = currentPos.distanceTo(DRAYNOR_FARMER_LOCATION);

        return distance > 10;
    }

    @Override
    public boolean execute() {
        task = "Walking to Draynor";

        WorldPosition currentPos = script.getWorldPosition();
        if (currentPos == null) {
            script.log("WARN", "Cannot get current position");
            return false;
        }

        script.getWalker().walkTo(DRAYNOR_FARMER_LOCATION);

        script.submitHumanTask(() -> {
            WorldPosition current = script.getWorldPosition();
            return current != null && current.distanceTo(DRAYNOR_FARMER_LOCATION) < 10;
        }, script.random(30000, 35000));

        return false;
    }
}
