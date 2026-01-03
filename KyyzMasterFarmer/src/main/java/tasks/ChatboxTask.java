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

        // Only activate if chatbox is actually closed (not based on chatDetected)
        try {
            return !script.getWidgetManager().getChatbox().isOpen();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean execute() {
        task = "Opening chatbox";

        try {
            Chatbox chatbox = script.getWidgetManager().getChatbox();
            if (chatbox != null) {
                chatbox.open();
                script.submitTask(() -> false, 500);
                // Don't set chatDetected = true here - ThieveTask will determine
                // if chat is actually readable (filter might still be wrong)
                return true;
            }
        } catch (Exception e) {
            script.log("WARN", "Failed to open chatbox: " + e.getMessage());
        }

        return false;
    }
}
