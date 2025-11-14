package client.dto;

import java.util.List;
import java.util.Map;

// temporary holder so ServerFacade can return something.
// I'll fill in real fields later once I see how the server formats stuff.

public class ListGamesResponse {

    // just storing raw list of maps for now
    public List<Map<String, Object>> games;

    public ListGamesResponse(List<Map<String, Object>> games) {
        this.games = games;
    }

    // might add getters/setters later if needed
}
