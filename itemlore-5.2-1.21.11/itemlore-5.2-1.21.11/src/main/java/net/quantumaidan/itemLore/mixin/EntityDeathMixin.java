package net.quantumaidan.itemLore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.quantumaidan.itemLore.ItemLore;
import net.quantumaidan.itemLore.util.statTrackLore;

@Mixin(LivingEntity.class)
public class EntityDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void onLivingDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity victim = (LivingEntity) (Object) this;

        ItemLore.LOGGER.info("[EntityDeathMixin] die(): victim={}, victimType={}",
                victim.getName().getString(),
                victim.getType().toString());

        ItemLore.LOGGER.info("[EntityDeathMixin] die(): damageSource.msgId={}", damageSource.getMsgId());

        ItemLore.LOGGER.info("[EntityDeathMixin] die(): source.getEntity()={}",
                (damageSource.getEntity() == null ? "null" : damageSource.getEntity().getType().toString()));

        ItemLore.LOGGER.info("[EntityDeathMixin] die(): source.getDirectEntity()={}",
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