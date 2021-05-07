package com.bb1.discord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public final class Config {
	
	private static Config CONFIG;
	private static final JsonParser PARSER = new JsonParser();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	public static final Config get() {
		if (CONFIG==null) {
			CONFIG = new Config();
		}
		return CONFIG;
	}
	
	private final String configFolder = "."+File.separatorChar+"config"+File.separatorChar;
	private JsonObject jsonObject;
	private boolean loaded = false;
	
	private Config() {
		File configPath = new File(configFolder);
		configPath.mkdirs();
		File config = getFile();
		if (config==null || !config.exists()) {
			if (create()) {
				this.jsonObject = getDefaults();
				writeConfig();
			}
			return;
		}
		Scanner s = null;
		try { s = new Scanner(config); } catch (FileNotFoundException ignore) { } // If the file doesn't exist it shouldn't have been provided
		ArrayList<String> r = new ArrayList<String>();
		while (s.hasNext()) {
	    	r.add(s.nextLine());
		}
		s.close();
		JsonElement jsonElement = PARSER.parse(String.join("", r));
		if (jsonElement.isJsonObject()) {
			this.jsonObject = jsonElement.getAsJsonObject();
		} else {
			this.jsonObject = getDefaults();
			writeConfig();
			return;
		}
		loaded = true;
	}
	
	public boolean isLoaded() {
		return this.loaded;
	}
	
	public String getToken() {
		JsonElement jsonElement = this.jsonObject.get("token");
		return (jsonElement.isJsonPrimitive()) ? (jsonElement.getAsJsonPrimitive().isString()) ? jsonElement.getAsString() : "" : "";
	}
	
	public String getPrefix() {
		JsonElement jsonElement = this.jsonObject.get("commandPrefix");
		return (jsonElement.isJsonPrimitive()) ? (jsonElement.getAsJsonPrimitive().isString()) ? jsonElement.getAsString() : "d!" : "d!";
	}
	// All the users with admin perms
	public long[] getAuthedUsers() {
		JsonElement jsonElement = this.jsonObject.get("authedUsers");
		if (!jsonElement.isJsonArray()) return new long[0];
		JsonArray jsonArray = jsonElement.getAsJsonArray();
		long[] longs = new long[jsonArray.size()];
		for (int i = 0; i < jsonArray.size(); i++) {
			JsonElement json = jsonArray.get(i);
			if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) continue;
			longs[i] = json.getAsLong();
		}
		return longs;
	}
	/**
	 * If the channel is authed it will wrap it into a {@link Channel} elsewise it returns an empty channel
	 */
	public Channel getChannel(@NotNull TextChannel textChannel) {
		if (textChannel==null) return Channel.createEmpty();
		JsonElement jsonElement = this.jsonObject.get("channels");
		if (!jsonElement.isJsonArray()) return Channel.createEmpty();
		JsonArray jsonArray = jsonElement.getAsJsonArray();
		final long id = textChannel.asServerChannel().get().getServer().getId();
		final long id2 = textChannel.getId();
		for (JsonElement json : jsonArray) {
			if (!json.isJsonObject()) continue;
			JsonObject jsonObject = json.getAsJsonObject();
			JsonElement server = jsonObject.get("server");
			if (server==null || !server.isJsonPrimitive() || !server.getAsJsonPrimitive().isNumber()) continue;
			long serverID = server.getAsLong();
			if (serverID!=id) continue;
			JsonElement channel = jsonObject.get("channel");
			if (channel==null || !channel.isJsonPrimitive() || !channel.getAsJsonPrimitive().isNumber()) continue;
			long channelID = channel.getAsLong();
			if (channelID!=id2) continue;
			JsonElement send = jsonObject.get("sendPerms");
			if (send==null || !send.isJsonPrimitive() || !send.getAsJsonPrimitive().isBoolean()) continue;
			boolean sendPerms = send.getAsBoolean();
			return new Channel() {
				
				@Override
				public void sendMessage(String message) {
					if (!canSendMessagesToMC()) return;
					textChannel.sendMessage(message);
				}
				
				@Override
				public boolean canSendMessagesToMC() {
					return sendPerms;
				}
				
				@Override
				public boolean canGetMessagesFromMC() {
					return true;
				}
				
			};
		}
		return Channel.createEmpty();
	}
	
	public Set<TextChannel> getChannels(@NotNull DiscordApi discordApi) {
		JsonElement jsonElement = this.jsonObject.get("channels");
		if (!jsonElement.isJsonArray()) return new HashSet<TextChannel>();
		Set<TextChannel> set = new HashSet<TextChannel>();
		JsonArray jsonArray = jsonElement.getAsJsonArray();
		for (JsonElement json : jsonArray) {
			if (!json.isJsonObject()) continue;
			JsonObject jsonObject = json.getAsJsonObject();
			JsonElement server = jsonObject.get("server");
			if (!server.isJsonPrimitive() || !server.getAsJsonPrimitive().isNumber()) continue;
			long serverID = server.getAsLong();
			JsonElement channel = jsonObject.get("channel");
			if (!channel.isJsonPrimitive() || !channel.getAsJsonPrimitive().isNumber()) continue;
			long channelID = channel.getAsLong();
			try {
				set.add(discordApi.getServerById(serverID).get().getTextChannelById(channelID).get());
			} catch (Exception e) {
				// Channel is not there or not cached
			}
		}
		return set;
	}
	
	public void addChannel(@NotNull TextChannel textChannel, boolean sendPerms) {
		JsonElement jsonElement = this.jsonObject.get("channels");
		if (!jsonElement.isJsonArray()) return;
		JsonArray jsonArray = jsonElement.getAsJsonArray();
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("server", textChannel.asServerChannel().get().getServer().getId());
		jsonObject.addProperty("channel", textChannel.getId());
		// If the channel can send messages to the server (adds read only channels)
		jsonObject.addProperty("sendPerms", sendPerms);
		jsonArray.add(jsonObject);
		this.jsonObject.add("channels", jsonArray);
	}
	
	public void removeChannel(@NotNull TextChannel textChannel) {
		JsonElement jsonElement = this.jsonObject.get("channels");
		if (!jsonElement.isJsonArray()) return;
		JsonArray jsonArray = jsonElement.getAsJsonArray();
		final long id = textChannel.asServerChannel().get().getServer().getId();
		final long id2 = textChannel.getId();
		for (JsonElement jsonElement2 : jsonArray) {
			try {
				JsonObject jsonObject = jsonElement2.getAsJsonObject();
				if (jsonObject.get("server").getAsLong()==id && jsonObject.get("channel").getAsLong()==id2) {
					jsonArray.remove(jsonElement2);
					break;
				}
			} catch (Exception e) {
				// If any exceptions occur its not a correct listing so we remove this index
				jsonArray.remove(jsonElement2);
			}
		}
		this.jsonObject.add("channels", jsonArray);
	}
	
	public void addAuthedUser(long id) {
		JsonElement jsonElement = this.jsonObject.get("authedUsers");
		if (!jsonElement.isJsonArray()) return;
		JsonArray jsonArray = jsonElement.getAsJsonArray();
		jsonArray.add(id);
		this.jsonObject.add("authedUsers", jsonArray);
	}
	
	public void removeAuthedUser(long id) {
		JsonElement jsonElement = this.jsonObject.get("authedUsers");
		if (!jsonElement.isJsonArray()) return;
		JsonArray jsonArray = jsonElement.getAsJsonArray();
		jsonArray.remove(new JsonPrimitive(id));
		this.jsonObject.add("authedUsers", jsonArray);
	}
	
	public final boolean writeConfig() {
		try {
			BufferedWriter b = new BufferedWriter(new PrintWriter(getFile()));
			b.write(GSON.toJson(jsonObject));
			b.flush();
			b.close();
			return true;
		} catch (IOException e) {
			System.err.println("Could not save DMF config");
			return false;
		}
	}
	
	private final boolean create() {
		File config = getFile();
		try {
			config.createNewFile();
			return true;
		} catch (IOException e) {
			System.err.println("Failed to create DMF config");
			return false;
		}
	}
	
	private final File getFile() {
		return new File(configFolder+"DMF.json");
	}
	
	private final JsonObject getDefaults() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("token", "ENTER_TOKEN_HERE");
		jsonObject.add("authedUsers", new JsonArray());
		jsonObject.addProperty("commandPrefix", "d!");
		jsonObject.add("channels", new JsonArray());
		return jsonObject;
	}
	
}
