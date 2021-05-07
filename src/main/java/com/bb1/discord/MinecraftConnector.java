package com.bb1.discord;
/**
 * A simple interface to make it so messages can be sent to all clients
 */
public interface MinecraftConnector {
	
	public void sendMessageToAllPlayers(String formattedString);
	
}
