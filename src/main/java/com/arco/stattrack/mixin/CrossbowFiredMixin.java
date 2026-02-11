package com.arco.stattrack.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.arco.stattrack.util.setLore;
import com.arco.stattrack.util.statTrackLore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// ... existing code ...

@Mixin(CrossbowItem.class)
public class CrossbowFiredMixin {

    @Inject(method = "performShooting", at = @At("TAIL"))
    private static void onShoot(Level level,
                                LivingEntity livingEntity,
                                InteractionHand interactionHand,
                                ItemStack itemStack,
                                float f,
                                float g,
                                LivingEntity livingEntity2,
                                CallbackInfo ci) {
        if (!(livingEntity instanceof ServerPlayer player)) return;

        ItemStack inHand = player.getItemInHand(interactionHand);

        setLore.applyNewLore(player, inHand);

        // "crossbow performed shooting" behavior
        statTrackLore.onArrowFired(player, inHand);
    }
}