package shared;

import org.json.JSONObject;
import server.Player;

public interface RecieveSendAction {

    void invoke(JSONObject json, Player player);
}
