package com.bb1.discord;

public interface Channel {
	
	public static Channel createEmpty() {
		return new Channel() {
			
			@Override
			public void sendMessage(String message) {
				return; 
			}
			
			@Override
			public boolean canSendMessagesToMC() {
				return false;
			}

			@Override
			public boolean canGetMessagesFromMC() {
				return false;
			}
			
		};
	}
	
	public boolean canGetMessagesFromMC();
	
	public boolean canSendMessagesToMC();
	
	public void sendMessage(String message);
	
}
