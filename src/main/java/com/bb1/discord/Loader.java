package com.bb1.discord;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;

public class Loader implements DedicatedServerModInitializer, MinecraftConnector {
	
	private static List<PlayerEntity> players = new ArrayList<PlayerEntity>();
	
	private boolean loadedLibs = false;
	
	public static void onJoin(PlayerEntity player) {
		players.add(player);
		DiscordBot.handleMessage(player.getName().asString()+" joined the game");
	}
	
	public static void onLeave(PlayerEntity player) {
		players.remove(player);
		DiscordBot.handleMessage(player.getName().asString()+" left the game");
	}
	
	@Override
	public void onInitializeServer() {
		loadLibs();
		while (!loadedLibs) { continue; }
		DiscordBot discordBot = new DiscordBot(this);
		discordBot.start();
		discordBot.sendMessage("🟢  Server online");
		ServerLifecycleEvents.SERVER_STOPPED.register(new ServerStopped() {
			
			@Override
			public void onServerStopped(MinecraftServer server) {
				discordBot.sendMessage("🔴  Server offline");
				discordBot.stop();
				Config.get().writeConfig();
			}
			
		});
	}
	
	private final String libsFolder = "."+File.separatorChar+"libs"+File.separatorChar;
	
	private void loadLibs() {
		new File(libsFolder).mkdirs();
		File javacord = new File(libsFolder+"javacord.jar");
		boolean done = true;
		if (!javacord.exists()) {
			try {
				done = false;
				javacord.createNewFile();
				BufferedInputStream in = new BufferedInputStream(new URL("https://github.com/Javacord/Javacord/releases/download/v3.3.0/javacord-3.3.0-shaded.jar").openStream());
				FileOutputStream fileOutputStream = new FileOutputStream(javacord);
				byte dataBuffer[] = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
					fileOutputStream.write(dataBuffer, 0, bytesRead);
				}
				fileOutputStream.flush();
				fileOutputStream.close();
				done = true;
				System.out.println("Downloaded required libs");
			} catch (IOException e) {
				System.err.println("Failed to download required libs");
			}
		}
		while (!done) {} // Pause here until done
		try {
			URL url = javacord.toURI().toURL();  
			ClassLoader scl = ClassLoader.getSystemClassLoader();
			if (scl instanceof URLClassLoader) {
				if(Arrays.asList(((URLClassLoader)scl).getURLs()).contains(url)) {
					loadedLibs = true;
	            	return; // Already loaded
	            }
				Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{java.net.URL.class});
				method.setAccessible(true);
				method.invoke(scl, new Object[]{url});
				loadedLibs = true;
			} else {
				// TODO: load with other class loaders
			}
		} catch (MalformedURLException | IllegalArgumentException | SecurityException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			System.err.println("Failed to load required classes from libs");
		}
	}

	@Override
	public void sendMessageToAllPlayers(String formattedString) {
		for (PlayerEntity playerEntity : players) {
			playerEntity.sendMessage(new LiteralText(formattedString), false);
		}
	}
	
}
