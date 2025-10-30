package tasks;

import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.Chatbox;
import utils.Task;

import static main.KyyzMasterFarmer.*;

public class ChatboxTask extends Task {

    public ChatboxTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone) {
            return false;
        }

        return !chatDetected;
    }

    @Override
    public boolean execute() {
        task = "Opening chatbox";
        script.log("INFO", "Chatbox closed - attempting to re-open...");

        try {
            Chatbox chatbox = script.getWidgetManager().getChatbox();
            if (chatbox != null) {
                chatbox.open();
                script.submitTask(() -> false, 500);
                script.log("INFO", "Opened chatbox!");
                chatDetected = true;
                return true;
            }
        } catch (Exception e) {
            script.log("WARN", "Failed to open chatbox: " + e.getMessage());
        }

        return false;
    }
}
