package com.arco.stattrack.util;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class setLore {

    @SuppressWarnings("null")
    public static boolean applyNewLore(Player player, ItemStack itemStack) {
        net.minecraft.world.item.component.ItemLore inputLore = itemStack.get(DataComponents.LORE);
        if (inputLore == null || inputLore.lines() == null || inputLore.lines().isEmpty()) {

            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                statTrackLore.setTrackedBy(itemStack, sp);
            }

            Component header = Component.literal("Stat Track")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false));

            Component trackedBy = Component.literal(" (Tracked by ")
                    .append(player.getDisplayName())
                    .append(")")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(false));

            net.minecraft.world.item.component.ItemLore newLoreComponent =
                    new net.minecraft.world.item.component.ItemLore(
                            new java.util.ArrayList<>(List.of(
                                    Component.empty().append(header).append(trackedBy)
                            )));

            itemStack.set(DataComponents.LORE, newLoreComponent);
            return true;
        }

        return false;
    }
}