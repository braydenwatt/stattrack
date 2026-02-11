package com.arco.stattrack.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import com.arco.stattrack.util.setLore;
import com.arco.stattrack.util.statTrackLore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ElytraFlightMixin {

    @Unique private double stattrack_lastX;
    @Unique private double stattrack_lastY;
    @Unique private double stattrack_lastZ;
    @Unique private boolean stattrack_hasLast;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        if (!player.isFallFlying()) {
            stattrack_hasLast = false;
            return;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        setLore.applyNewLore(player, chest);
        if (!statTrackLore.hasLore(chest)) {
            stattrack_hasLast = false;
            return;
        }

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        if (!stattrack_hasLast) {
            stattrack_lastX = x;
            stattrack_lastY = y;
            stattrack_lastZ = z;
            stattrack_hasLast = true;
            return;
        }

        double dx = x - stattrack_lastX;
        double dy = y - stattrack_lastY;
        double dz = z - stattrack_lastZ;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int blocks = (int) Math.floor(dist);

        stattrack_lastX = x;
        stattrack_lastY = y;
        stattrack_lastZ = z;

        if (blocks > 0) {
            statTrackLore.onBlocksFlown(player, chest, blocks);
        }
    }
}