package tasks;

import com.osmb.api.script.Script;
import utils.Task;

import static main.MithrilDartFletcher.*;

public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        // Only run once at the beginning
        return !hasReqs;
    }

    @Override
    public boolean execute() {
        task = "Setup";

        // Always true because requirements are met by design
        hasReqs = true;

        script.log("SETUP", "Setup completed. Ready to craft darts.");
        return true;
    }
}
