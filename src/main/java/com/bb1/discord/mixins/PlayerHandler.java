package com.bb1.discord.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bb1.discord.Loader;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(PlayerManager.class)
public class PlayerHandler {
	
	@Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("TAIL"))
	public void inject(ClientConnection connection, ServerPlayerEntity player, CallbackInfo callbackInfo) {
		Loader.onJoin(player);
	}
	
	@Inject(method = "remove(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("TAIL"))
	public void inject(ServerPlayerEntity player, CallbackInfo callbackInfo) {
		Loader.onLeave(player);
	}
	
}
