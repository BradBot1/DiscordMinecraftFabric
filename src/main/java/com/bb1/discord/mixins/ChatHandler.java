package com.bb1.discord.mixins;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bb1.discord.DiscordBot;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayNetworkHandler.class)
public class ChatHandler {
	
	@Shadow public ServerPlayerEntity player;
	
	@Inject(method = "onGameMessage(Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;)V", at = @At("TAIL"))
	public void onGameMessage(ChatMessageC2SPacket packet, CallbackInfo callbackInfo) {
		String string = StringUtils.normalizeSpace(packet.getChatMessage());
		if (!string.startsWith("/")) {
			DiscordBot.handleMessage(player, string);
		}
	}
	
}
