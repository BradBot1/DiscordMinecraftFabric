package com.bb1.discord;

import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import net.minecraft.entity.player.PlayerEntity;
/**
 * Copyright 2021 BradBot_1
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public final class DiscordBot {
	
	private static DiscordBot discordBot;
	
	public static void handleMessage(PlayerEntity playerEntity, String message) {
		discordBot.sendMessage(playerEntity, message);
	}
	
	public static void handleMessage(String message) {
		discordBot.sendMessage(message);
	}
	
	private DiscordApi discordApi;
	private final MinecraftConnector minecraftConnector;
	private final Config config;
	private Thread thread;
	
	public DiscordBot(MinecraftConnector minecraftConnector) {
		if (minecraftConnector==null) throw new IllegalArgumentException("The MinecraftConnector cannot be null");
		this.minecraftConnector = minecraftConnector;
		this.config = Config.get();
		discordBot = this;
	}
	
	public void start() {
		if (!config.isLoaded()) return;
		if (discordApi!=null) throw new IllegalStateException("The bot has already been started in this instance");
		DiscordApiBuilder discordApiBuilder = new DiscordApiBuilder().setToken(config.getToken()).addMessageCreateListener(new MessageCreateListener() {
			
			@Override
			public void onMessageCreate(MessageCreateEvent event) {
				if (event.getMessageAuthor().isBotUser()) return; // We don't handle bots 
				String msg = event.getMessageContent();
				if (msg.startsWith(config.getPrefix())) {
					// It's a command
					final long userID = event.getMessageAuthor().getId();
					for (long authedUserID : config.getAuthedUsers()) {
						if (authedUserID==userID) {
							String[] split = msg.replaceFirst(config.getPrefix(), "").split(" ");
							switch(split[0]) {
							case "register":
								boolean sendPerms = false;
								if (split.length>1) { // Params
									sendPerms = Boolean.parseBoolean(split[1]);
								} 
								config.addChannel(event.getChannel(), sendPerms);
								event.getChannel().sendMessage("Channel registered with"+((sendPerms) ? "" : "out")+" sending permissions");
								break;
							case "unregister":
								if (split.length>1) { // Params
									String[] split2 = split[1].split(":");
									if (split2.length<2) {
										event.getChannel().sendMessage("Invalid format! Please follow the format serverID:channelID");
										break;
									}
									try {
										config.removeChannel(discordApi.getServerById(split2[0]).get().getTextChannelById(split2[1]).get());
										event.getChannel().sendMessage("Removed the channel "+split[1]);
									} catch (Exception e) {
										event.getChannel().sendMessage("Failed to remove requested channel");
									}
									break;
								}
								config.removeChannel(event.getChannel());
								event.getChannel().sendMessage("Channel unregistered");
								break;
							case "auth":
								if (split.length>1) { // Params
									List<User> list = event.getMessage().getMentionedUsers();
									if (list==null || list.size()<1) break;
									for (User user : list) {
										config.addAuthedUser(user.getId());
										event.getChannel().sendMessage("Authorised the user "+user.getId());
									}
								}
								event.getChannel().sendMessage("No users specified");
								break;
							case "unauth":
								if (split.length>1) { // Params
									List<User> list = event.getMessage().getMentionedUsers();
									if (list==null || list.size()<1) break;
									for (User user : list) {
										config.removeAuthedUser(user.getId());
										event.getChannel().sendMessage("Unauthorised the user "+user.getId());
									}
									break;
								}
								event.getChannel().sendMessage("No users specified");
								break;
							}
							break;
						}
					}
				} else {
					Channel channel = config.getChannel(event.getChannel());
					if (channel.canSendMessagesToMC()) {
						final String send = formatForMC(event);
						minecraftConnector.sendMessageToAllPlayers(send);
						final String send2 = "["+event.getServer().get().getName()+"]"+send;
						config.getChannels(discordApi).forEach((t) -> {
							if (t.getId()==event.getChannel().getId()) return;
							t.sendMessage(send2);
						});
					}
				}
			}
			
		}).setWaitForServersOnStartup(true);
		this.thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				discordApi = discordApiBuilder.login().join();
			}
			
		}, "BOT_THREAD");
		this.thread.start();
		sendMessage("🟢  Bot started");
	}
	
	@SuppressWarnings("deprecation")
	public void stop() {
		if (discordApi==null) return;
		sendMessage("🔴  Bot stopped");
		discordApi.disconnect();
		discordApi.getThreadPool().getScheduler().shutdown();
		this.thread.stop();
		discordApi=null;
	}
	
	public void sendMessage(PlayerEntity sender, String text) {
		sendMessage("["+sender.getName().asString()+"]: "+text);
	}
	
	public void sendMessage(String text) {
		for (TextChannel channel : config.getChannels(discordApi)) {
			channel.sendMessage(formatForDiscord(text));
		}
	}
	
	public String formatForMC(MessageCreateEvent event) {
		return "["+event.getMessageAuthor().getDisplayName()+"]: "+event.getMessageContent();
	}
	
	public String formatForDiscord(String input) {
		return input.replaceAll("\\*", "\\\\*").replaceAll("\\_", "\\\\_").replaceAll("\\@", "\\\\@");
	}
	
}
