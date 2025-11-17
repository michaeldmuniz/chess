package client.ui;

import model.GameData;

public class PostloginResult {

    // Set when user logs out from postlogin UI
    public boolean loggedOut = false;

    // Set when user quits the entire program
    public boolean quit = false;

    // --- Gameplay transition flags ---

    // Did the user enter a game (either by re-enter or successful join)?
    public boolean enteredGame = false;

    // Which game did they join?
    public GameData joinedGame = null;

    // The color they are playing as ("WHITE" or "BLACK")
    public String joinColor = null;

    public PostloginResult() {}
}
