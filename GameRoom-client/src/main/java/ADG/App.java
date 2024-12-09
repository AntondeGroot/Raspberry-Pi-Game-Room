package ADG;

import com.google.gwt.core.client.EntryPoint;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class App implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side GameRoom service.
	 */
//	private final GameRoomServiceAsync gameRoomService = GWT.create(GameRoomServiceAsync.class);

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		// create a cookie for each player
		Cookie.createPlayerIdCookie();

		// Initialize the GameLobby when the application starts
		GameLobby gameLobby = new GameLobby();
		gameLobby.onModuleLoad();
	}
}
