package net.quantumaidan.itemLore.mixin;

import net.quantumaidan.itemLore.util.statTrackLore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.quantumaidan.itemLore.ItemLore;

@Mixin(ServerPlayer.class)
public class PlayerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayer victim = (ServerPlayer) (Object) this;

        ItemLore.LOGGER.info("[EntityDeathMixin] ServerPlayer die(): victim={}, msgId={}",
                victim.getName().getString(),
                damageSource.getMsgId());

        ItemLore.LOGGER.info("[EntityDeathMixin] ServerPlayer die(): source.getEntity()={}, source.getDirectEntity()={}",
                (damageSource.getEntity() == null ? "null" : damageSource.getEntity().getType().toString()),
                (damageSource.getDirectEntity() == null ? "null" : damageSource.getDirectEntity().getType().toString()));

        if (damageSource.getEntity() instanceof ServerPlayer player) {
            ItemLore.LOGGER.info("[EntityDeathMixin] die(): killer is ServerPlayer={}, heldItem={}",
                    player.getName().getString(),
                    player.getMainHandItem().getHoverName().getString());

            statTrackLore.onEntityKilledWithLoredTool(player, player.level(), victim, player.getMainHandItem());
        } else {
            ItemLore.LOGGER.info("[EntityDeathMixin] die(): killer is not a ServerPlayer (this is common for projectile kills)");
        }
    }
}