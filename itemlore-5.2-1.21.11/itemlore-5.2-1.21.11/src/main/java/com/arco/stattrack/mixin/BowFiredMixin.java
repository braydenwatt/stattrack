package com.arco.stattrack.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.arco.stattrack.util.setLore;
import com.arco.stattrack.util.statTrackLore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// ... existing code ...

@Mixin(BowItem.class)
public class BowFiredMixin {

    @Inject(method = "releaseUsing", at = @At("TAIL"))
    private void onRelease(ItemStack itemStack, Level level, LivingEntity livingEntity, int i, CallbackInfoReturnable<Boolean> cir) {
        if (!(livingEntity instanceof ServerPlayer player)) return;

        ItemStack inHand = player.getMainHandItem();

        setLore.applyNewLore(player, inHand);

        // "player released bow" behavior
        statTrackLore.onArrowFired(player, inHand);
    }
}