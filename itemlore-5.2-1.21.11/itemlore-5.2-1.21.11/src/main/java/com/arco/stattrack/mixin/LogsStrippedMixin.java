package com.arco.stattrack.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.quantumaidan.itemLore.config.itemLoreConfig;
import net.quantumaidan.itemLore.util.setLore;
import net.quantumaidan.itemLore.util.statTrackLore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AxeItem.class)
public class LogsStrippedMixin {

    @Inject(method = "useOn", at = @At("TAIL"))
    private void onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        InteractionResult result = cir.getReturnValue();
        if (!result.consumesAction()) return;

        ItemStack inHand = context.getItemInHand();
        if (itemLoreConfig.forceLoreMode != itemLoreConfig.ForceLoreMode.OFF) {
            setLore.applyNewLore(player, inHand);
        }

        statTrackLore.onLogStripped(player, inHand);
    }
}