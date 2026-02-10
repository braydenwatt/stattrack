package com.arco.stattrack.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.arco.stattrack.ItemLore;

public class statTrackLore {

    // ---------------------------
    // StatTrack (new)
    // ---------------------------

    private static final String NBT_STATTRACK_SELECTED = "stattrack_selected";
    private static final String NBT_STATTRACK_STATS = "stattrack_stats";

    private static final String STAT_PLAYER_KILLS = "player_kills";
    private static final String STAT_MOB_KILLS = "mob_kills";
    private static final String STAT_BLOCKS_MINED = "blocks_mined";
    private static final String STAT_DAMAGE_ABSORBED = "damage_absorbed";
    private static final String STAT_BLOCKS_FLOWN = "blocks_flown";
    private static final String STAT_ARROWS_FIRED = "arrows_fired";
    private static final String STAT_LOGS_STRIPPED = "logs_stripped";
    private static final String STAT_MOST_KILLED = "most_killed";

    private static final String NBT_PLAYER_KILL_STATS = "player_kill_stats";
    private static final String STAT_MOST_KILLED_PLAYER = "most_killed_player";
    public static final String NBT_STATTRACK_TRACKED_BY_UUID = "stattrack_tracked_by_uuid";

    public static boolean isSupportedTrackedStat(String stat) {
        return STAT_PLAYER_KILLS.equals(stat)
                || STAT_MOB_KILLS.equals(stat)
                || STAT_BLOCKS_MINED.equals(stat)
                || STAT_DAMAGE_ABSORBED.equals(stat)
                || STAT_BLOCKS_FLOWN.equals(stat)
                || STAT_ARROWS_FIRED.equals(stat)
                || STAT_LOGS_STRIPPED.equals(stat)
                || STAT_MOST_KILLED.equals(stat)
                || STAT_MOST_KILLED_PLAYER.equals(stat);
    }

    public static void setTrackedBy(ItemStack stack, ServerPlayer player) {
        if (stack == null || stack.isEmpty() || player == null) return;

        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag customData = (nbtComp != null) ? nbtComp.copyTag() : new CompoundTag();

        customData.putString(NBT_STATTRACK_TRACKED_BY_UUID, player.getUUID().toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
    }

    public static boolean canPlayerUpdateStats(ItemStack stack, ServerPlayer player) {
        if (stack == null || stack.isEmpty() || player == null) return false;

        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComp == null) return false;

        CompoundTag customData = nbtComp.copyTag();
        String trackedBy = customData.getString(NBT_STATTRACK_TRACKED_BY_UUID).orElse(null);
        if (trackedBy == null || trackedBy.isBlank()) return false;

        return trackedBy.equals(player.getUUID().toString());
    }

    private static final String UID_PREFIX = "Stat Track";

    private static ChatFormatting statColor(String key) {
        return switch (key) {
            case STAT_PLAYER_KILLS -> ChatFormatting.RED;
            case STAT_MOB_KILLS -> ChatFormatting.DARK_GREEN;
            case STAT_BLOCKS_MINED -> ChatFormatting.GOLD;
            case STAT_DAMAGE_ABSORBED -> ChatFormatting.AQUA;
            case STAT_BLOCKS_FLOWN -> ChatFormatting.LIGHT_PURPLE;
            case STAT_ARROWS_FIRED -> ChatFormatting.YELLOW;
            case STAT_LOGS_STRIPPED -> ChatFormatting.DARK_AQUA;
            case STAT_MOST_KILLED -> ChatFormatting.BLUE;
            case STAT_MOST_KILLED_PLAYER -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.DARK_GRAY;
        };
    }

    private static Component findUidLine(ItemStack stack) {
        net.minecraft.world.item.component.ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;
        List<Component> lines = lore.lines();
        if (lines == null) return null;

        for (Component line : lines) {
            if (line == null) continue;
            String s = line.getString();
            if (s != null && s.startsWith(UID_PREFIX)) {
                return line;
            }
        }
        return null;
    }
    private static void appendSelectedStatLine(ItemStack item, List<Component> newLines) {
        String selected = getSelectedTrackedStat(item);
        if (selected == null || selected.isBlank()) return;

        Number value = getTrackedStatValue(item, selected);
        newLines.add(
                Component.literal(prettyStatName(selected) + ": " + value)
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY))
        );
    }

    @SuppressWarnings("null")
    public static void setSelectedTrackedStat(ItemStack stack, String stat) {
        if (stack == null || stack.isEmpty()) return;

        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag customData = (nbtComp != null) ? nbtComp.copyTag() : new CompoundTag();

        customData.putString(NBT_STATTRACK_SELECTED, stat);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
    }

    @SuppressWarnings("null")
    public static String getSelectedTrackedStat(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComp == null) return null;

        CompoundTag customData = nbtComp.copyTag();
        return customData.getString(NBT_STATTRACK_SELECTED).orElse(null);
    }

    @SuppressWarnings("null")
    private static CompoundTag getOrCreateStatTrackStats(CompoundTag customData) {
        Optional<CompoundTag> opt = customData.getCompound(NBT_STATTRACK_STATS);
        CompoundTag stats = opt.orElse(new CompoundTag());
        if (opt.isEmpty()) {
            customData.put(NBT_STATTRACK_STATS, stats);
        }
        return stats;
    }

    @SuppressWarnings("null")
    private static void incrementStatInt(ItemStack stack, String statKey, int delta) {
        if (stack == null || stack.isEmpty() || delta == 0) return;

        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag customData = (nbtComp != null) ? nbtComp.copyTag() : new CompoundTag();
        CompoundTag stats = getOrCreateStatTrackStats(customData);

        int cur = stats.getInt(statKey).orElse(0);
        stats.putInt(statKey, Math.max(0, cur + delta));

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
    }

    @SuppressWarnings("null")
    private static void incrementStatFloat(ItemStack stack, String statKey, float delta) {
        if (stack == null || stack.isEmpty() || delta == 0.0f) return;

        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag customData = (nbtComp != null) ? nbtComp.copyTag() : new CompoundTag();
        CompoundTag stats = getOrCreateStatTrackStats(customData);

        float cur = stats.getFloat(statKey).orElse(0.0f);
        float next = Math.max(0.0f, cur + delta);
        next = Math.round(next * 10.0f) / 10.0f; // keep 1 decimal place

        stats.putFloat(statKey, next);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
    }

    @SuppressWarnings("null")
    public static Number getTrackedStatValue(ItemStack stack, String statKey) {
        if (stack == null || stack.isEmpty()) return 0;

        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComp == null) return 0;

        CompoundTag customData = nbtComp.copyTag();
        Optional<CompoundTag> opt = customData.getCompound(NBT_STATTRACK_STATS);
        if (opt.isEmpty()) return 0;

        CompoundTag stats = opt.get();
        if (STAT_DAMAGE_ABSORBED.equals(statKey)) {
            return stats.getFloat(statKey).orElse(0.0f);
        }
        return stats.getInt(statKey).orElse(0);
    }

    public static void refreshLore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !hasLore(stack)) return;

        // Reuse the existing lore update paths; they now append StatTrack section too.
        CustomData nbt = stack.get(DataComponents.CUSTOM_DATA);
        if (nbt != null) {
            CompoundTag customData = nbt.copyTag();
            if (customData.getCompound("armor_stats").isPresent()) {
                updateArmorLore(stack);
                return;
            }
        }
        updateItemLore(stack);
    }

    private static String prettyStatName(String key) {
        return switch (key) {
            case STAT_PLAYER_KILLS -> "Player Kills";
            case STAT_MOB_KILLS -> "Mob Kills";
            case STAT_BLOCKS_MINED -> "Blocks Mined";
            case STAT_DAMAGE_ABSORBED -> "Damage Absorbed";
            case STAT_BLOCKS_FLOWN -> "Blocks Flown";
            case STAT_ARROWS_FIRED -> "Arrows Fired";
            case STAT_LOGS_STRIPPED -> "Logs Stripped";
            case STAT_MOST_KILLED -> "Most Killed";
            case STAT_MOST_KILLED_PLAYER -> "Most Killed Player";
            default -> key;
        };
    }

    private static void appendStatTrackSection(ItemStack item, List<Component> newLines) {
        String selected = getSelectedTrackedStat(item);
        if (selected == null || selected.isBlank()) return;

        if (STAT_MOST_KILLED_PLAYER.equals(selected)) {
            CustomData nbtComp = item.get(DataComponents.CUSTOM_DATA);
            CompoundTag customData = (nbtComp != null) ? nbtComp.copyTag() : new CompoundTag();

            Optional<CompoundTag> opt = customData.getCompound(NBT_PLAYER_KILL_STATS);
            if (opt.isEmpty()) {
                newLines.add(Component.literal(prettyStatName(selected) + ": None")
                        .setStyle(Style.EMPTY.withColor(statColor(selected)).withItalic(false)));
                return;
            }

            CompoundTag stats = opt.get();
            String bestName = "";
            int bestCount = 0;

            for (String key : stats.keySet()) {
                int v = stats.getInt(key).orElse(0);
                if (v > bestCount) {
                    bestCount = v;
                    bestName = key;
                }
            }

            String text = (bestCount > 0)
                    ? (prettyStatName(selected) + ": " + bestName + " (" + bestCount + ")")
                    : (prettyStatName(selected) + ": None");

            newLines.add(Component.literal(text)
                    .setStyle(Style.EMPTY.withColor(statColor(selected)).withItalic(false)));
            return;
        }

        if (STAT_MOST_KILLED.equals(selected)) {
            Map<String, Integer> killStats = getKillStats(item);

            String mostKilled = "";
            int maxCnt = 0;
            for (Map.Entry<String, Integer> entry : killStats.entrySet()) {
                if (entry.getValue() > maxCnt) {
                    maxCnt = entry.getValue();
                    mostKilled = entry.getKey();
                }
            }

            String text = (maxCnt > 0)
                    ? (prettyStatName(selected) + ": " + mostKilled + " (" + maxCnt + ")")
                    : (prettyStatName(selected) + ": None");

            newLines.add(
                    Component.literal(text)
                            .setStyle(Style.EMPTY.withColor(statColor(selected)).withItalic(false))
            );
            return;
        }

        Number value = getTrackedStatValue(item, selected);

        newLines.add(
                Component.literal(prettyStatName(selected) + ": " + value)
                        .setStyle(Style.EMPTY.withColor(statColor(selected)).withItalic(false))
        );
    }

    /**
     * Checks if an ItemStack has lore.
     *
     * @param itemStack The item stack to check.
     * @return true if the item has lore, false otherwise.
     */
    public static boolean hasLore(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        net.minecraft.world.item.component.ItemLore lore = itemStack.get(DataComponents.LORE);
        return lore != null && !lore.lines().isEmpty();
    }

    /**
     * Gets the mining stats for a tool.
     *
     * @param tool The tool item stack.
     * @return A map of block types to mined counts.
     */
    @SuppressWarnings("null")
    public static Map<String, Integer> getMiningStats(ItemStack tool) {
        if (!hasLore(tool))
            return Collections.emptyMap();

        CustomData nbt = tool.get(DataComponents.CUSTOM_DATA);
        if (nbt == null)
            return Collections.emptyMap();

        CompoundTag customData = nbt.copyTag();
        Optional<CompoundTag> statsOpt = customData.getCompound("mining_stats");
        if (statsOpt.isEmpty())
            return Collections.emptyMap();

        CompoundTag stats = statsOpt.get();
        Map<String, Integer> result = new HashMap<>();
        for (String key : stats.keySet()) {
            int val = stats.getInt(key).orElse(0);
            if (val > 0)
                result.put(key, val);
        }
        return result;
    }

    /**
     * Gets the item type.
     *
     * @param item The tool item.
     * @return The item type: "pickaxe", "axe", "shovel", "hoe", "sword", "bow", or
     *         null if not a tool.
     */
    public static String getItemType(Item item) {
        if (item == null) {
            return null;
        }

        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        String[] parts = path.split("_");
        if (parts.length == 0)
            return null;
        String last = parts[parts.length - 1];
        if ("pickaxe".equals(last) || "axe".equals(last) || "shovel".equals(last) || "hoe".equals(last)
                || "sword".equals(last) || "bow".equals(last) || "crossbow".equals(last)) {
            return last;
        }
        return null;
    }

    /**
     * Gets the kill stats for a tool.
     *
     * @param tool The tool item stack.
     * @return A map of mob types to killed counts.
     */
    public static Map<String, Integer> getKillStats(ItemStack tool) {
        if (!hasLore(tool))
            return Collections.emptyMap();

        CustomData nbt = tool.get(DataComponents.CUSTOM_DATA);
        if (nbt == null)
            return Collections.emptyMap();

        CompoundTag customData = nbt.copyTag();
        Optional<CompoundTag> statsOpt = customData.getCompound("kill_stats");
        if (statsOpt.isEmpty())
            return Collections.emptyMap();

        CompoundTag stats = statsOpt.get();
        Map<String, Integer> result = new HashMap<>();
        for (String key : stats.keySet()) {
            @SuppressWarnings("null")
            int val = stats.getInt(key).orElse(0);
            if (val > 0)
                result.put(key, val);
        }
        return result;
    }

    /**
     * Checks if an item is a mining tool.
     *
     * @param item The tool item.
     * @return true if it's a mining tool.
     */
    public static boolean isMiningTool(Item item) {
        String type = getItemType(item);
        return ("pickaxe".equals(type) || "axe".equals(type) || "shovel".equals(type) || "hoe".equals(type));
    }

    /**
     * Checks if an item is an armor item.
     *
     * @param item The item.
     * @return true if it's armor (has armor stats).
     */
    public static boolean isArmor(Item item) {
        // Check if this item has armor stats stored
        return !getArmorStats(item.getDefaultInstance()).isEmpty();
    }

    /**
     * Checks if an item is an attack tool.
     *
     * @param item The tool item.
     * @return true if it's an attack tool.
     */
    public static boolean isAttackTool(Item item) {
        String type = getItemType(item);
        return ("sword".equals(type) || "axe".equals(type) || "bow".equals(type) || "crossbow".equals(type));
    }

    /**
     * Handles when a block is broken with a tool that has lore.
     * Tracks mining stats and updates the item's lore.
     *
     * @param blockPos   The position of the broken block.
     * @param blockState The state of the broken block.
     * @param tool       The tool used to break the block.
     */
    @SuppressWarnings("null")
    public static void onBlockBrokenWithLoredTool(ServerPlayer player, BlockPos blockPos, BlockState blockState, ItemStack tool) {
        if (!hasLore(tool)) {
            return;
        }
        if (!canPlayerUpdateStats(tool, player)) {
            return;
        }

        String minedKey = getMinedKey(blockState.getBlock());

        CustomData nbtComp = tool.get(DataComponents.CUSTOM_DATA);
        CompoundTag customData = (nbtComp != null) ? nbtComp.copyTag() : new CompoundTag();

        Optional<CompoundTag> optionalStats = customData.getCompound("mining_stats");
        CompoundTag stats = optionalStats.orElse(new CompoundTag());
        if (optionalStats.isEmpty()) {
            customData.put("mining_stats", stats);
        }
        int count = stats.getInt(minedKey).orElse(0) + 1;
        stats.putInt(minedKey, count);
        customData.put("mining_stats", stats);

        tool.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

        incrementStatInt(tool, STAT_BLOCKS_MINED, 1);

        updateItemLore(tool);
    }

    @SuppressWarnings("null")
    public static void onEntityKilledWithLoredTool(ServerPlayer player, Level world, LivingEntity entity, ItemStack tool) {
        ItemLore.LOGGER.info("[statTrackLore] onEntityKilledWithLoredTool: killer={}, victimType={}, tool={}",
                (player == null ? "null" : player.getName().getString()),
                (entity == null ? "null" : entity.getType().toString()),
                (tool == null ? "null" : tool.getHoverName().getString()));

        if (!hasLore(tool)) {
            ItemLore.LOGGER.info("[statTrackLore] kill: tool has no lore -> not tracking");
            return;
        }

        boolean canUpdate = canPlayerUpdateStats(tool, player);
        ItemLore.LOGGER.info("[statTrackLore] kill: canPlayerUpdateStats={}", canUpdate);
        if (!canUpdate) {
            return;
        }

        if (entity instanceof ServerPlayer killedPlayer) {
            String nameKey = killedPlayer.getScoreboardName();
            ItemLore.LOGGER.info("[statTrackLore] kill: victim is player, nameKey={}", nameKey);

            incrementStatInt(tool, STAT_PLAYER_KILLS, 1);

            CustomData nbtComp = tool.get(DataComponents.CUSTOM_DATA);
            CompoundTag customData = (nbtComp != null) ? nbtComp.copyTag() : new CompoundTag();

            Optional<CompoundTag> opt = customData.getCompound(NBT_PLAYER_KILL_STATS);
            CompoundTag playerStats = opt.orElse(new CompoundTag());
            if (opt.isEmpty()) {
                customData.put(NBT_PLAYER_KILL_STATS, playerStats);
            }

            int cur = playerStats.getInt(nameKey).orElse(0);
            playerStats.putInt(nameKey, cur + 1);
            customData.put(NBT_PLAYER_KILL_STATS, playerStats);

            tool.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

            ItemLore.LOGGER.info("[statTrackLore] kill: player_kill_stats updated {} -> {}", nameKey, (cur + 1));
            ItemLore.LOGGER.info("[statTrackLore] kill: player_kill_stats keys={}", playerStats.keySet());
        } else {
            incrementStatInt(tool, STAT_MOB_KILLS, 1);
        }

        // ... existing code that updates kill_stats and calls updateItemLore(tool) ...
        String killKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        killKey = killKey.substring(0, 1).toUpperCase() + killKey.substring(1);

        CustomData nbtComp = tool.get(DataComponents.CUSTOM_DATA);
        CompoundTag customData = (nbtComp != null) ? nbtComp.copyTag() : new CompoundTag();

        Optional<CompoundTag> optionalStats = customData.getCompound("kill_stats");
        CompoundTag stats = optionalStats.orElse(new CompoundTag());
        if (optionalStats.isEmpty()) {
            customData.put("kill_stats", stats);
        }
        int count = stats.getInt(killKey).orElse(0) + 1;
        stats.putInt(killKey, count);
        customData.put("kill_stats", stats);

        tool.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

        updateItemLore(tool);
        ItemLore.LOGGER.info("[statTrackLore] kill: updated kill_stats {} -> {}", killKey, count);
    }

    @SuppressWarnings("null")
    public static void onArmorPiecePreventedDamage(ServerPlayer player, ItemStack armorPiece, float damagePrevented) {
        if (!hasLore(armorPiece)) {
            return;
        }
        if (!canPlayerUpdateStats(armorPiece, player)) {
            return;
        }

        CustomData nbtComp = armorPiece.get(DataComponents.CUSTOM_DATA);
        CompoundTag customData = (nbtComp != null) ? nbtComp.copyTag() : new CompoundTag();

        Optional<CompoundTag> statsOpt = customData.getCompound("armor_stats");
        CompoundTag stats = statsOpt.orElse(new CompoundTag());
        if (statsOpt.isEmpty()) {
            customData.put("armor_stats", stats);
        }

        float currentPrevention = stats.getFloat("damage_prevented").orElse(0.0f);
        float newPrevention = currentPrevention + damagePrevented;
        newPrevention = Math.round(newPrevention * 10.0f) / 10.0f;
        stats.putFloat("damage_prevented", newPrevention);

        armorPiece.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

        incrementStatFloat(armorPiece, STAT_DAMAGE_ABSORBED, damagePrevented);

        updateArmorLore(armorPiece);
    }
    /**
     * Handles damage prevented by armor set (unused currently - distributed to
     * pieces).
     *
     * @param player          The player whose armor prevented damage.
     * @param damagePrevented The total damage prevented.
     */
    public static void onDamagePreventedByArmor(net.minecraft.server.level.ServerPlayer player, float damagePrevented) {
        ItemLore.LOGGER.info("[statTrackLore] onDamagePreventedByArmor called - damagePrevented: {}", damagePrevented);
        // This method is called but currently unused since we're distributing to
        // individual pieces
        // Could be used for total armor stats if needed
    }

    /**
     * Gets the armor stats for an armor piece.
     *
     * @param armorPiece The armor item stack.
     * @return A map of armor stats.
     */
    public static Map<String, Float> getArmorStats(ItemStack armorPiece) {
        if (!hasLore(armorPiece))
            return Collections.emptyMap();

        CustomData nbt = armorPiece.get(DataComponents.CUSTOM_DATA);
        if (nbt == null)
            return Collections.emptyMap();

        CompoundTag customData = nbt.copyTag();
        Optional<CompoundTag> statsOpt = customData.getCompound("armor_stats");
        if (statsOpt.isEmpty())
            return Collections.emptyMap();

        CompoundTag stats = statsOpt.get();
        Map<String, Float> result = new HashMap<>();
        for (String key : stats.keySet()) {
            @SuppressWarnings("null")
            float val = stats.getFloat(key).orElse(0.0f);
            if (val > 0)
                result.put(key, val);
        }
        return result;
    }

    /**
     * Updates the armor item's lore with damage prevention stats.
     *
     * @param armorPiece The armor item stack.
     */
    @SuppressWarnings("null")
    private static void updateArmorLore(ItemStack armorPiece) {
        net.minecraft.world.item.component.ItemLore existingLore = armorPiece.get(DataComponents.LORE);
        if (existingLore == null)
            return;

        List<Component> newLines = new ArrayList<>();

        Component uidLine = findUidLine(armorPiece);
        if (uidLine != null) {
            newLines.add(uidLine);
        }

        // NEW: StatTrack section
        appendStatTrackSection(armorPiece, newLines);

        armorPiece.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(newLines));
    }

    /**
     * Gets the key for the block based on its ID path.
     * Tracks all blocks broken.
     *
     * @param block The block broken.
     * @return The key for stats.
     */
    private static String getMinedKey(Block block) {
        @SuppressWarnings("null")
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        String name = id.getPath();
        // Capitalize the path
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Gets the base key for grouping in stats display.
     *
     * @param key The raw key from stats.
     * @return The base key for grouping.
     */
    public static String getBaseKey(String key) {
        if (key.contains("_ore")) {
            String base = key.replace("deepslate_", "").toLowerCase();
            base = base.replace("_ore", "");
            return base.substring(0, 1).toUpperCase() + base.substring(1);
        } else {
            return key;
        }
    }

    /**
     * Updates the item's lore with combined stats.
     * Shows blocks mined and mobs killed in gray text, or damage prevented for
     * armor.
     * For weapons, also shows the most killed mob type.
     *
     * @param item The item stack.
     */
    @SuppressWarnings("null")
    private static void updateItemLore(ItemStack item) {
        net.minecraft.world.item.component.ItemLore existingLore = item.get(DataComponents.LORE);
        if (existingLore == null)
            return;
        List<Component> existingLines = existingLore.lines();

        List<Component> newLines = new ArrayList<>();

        Component uidLine = findUidLine(item);
        if (uidLine != null) {
            newLines.add(uidLine);
        }

        CustomData nbt = item.get(DataComponents.CUSTOM_DATA);
        if (nbt != null) {
            CompoundTag customData = nbt.copyTag();
            if (customData.getCompound("armor_stats").isPresent()) {
                updateArmorLore(item);
                return;
            }
        }

        // NEW: StatTrack section
        appendStatTrackSection(item, newLines);

        item.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(newLines));
    }

    // Make these callable from mixins
    public static void onArrowFired(ServerPlayer player, ItemStack stack) {
        if (!hasLore(stack)) return;
        if (!canPlayerUpdateStats(stack, player)) return;

        incrementStatInt(stack, STAT_ARROWS_FIRED, 1);
        refreshLore(stack);
    }

    public static void onLogStripped(ServerPlayer player, ItemStack stack) {
        if (!hasLore(stack)) return;
        if (!canPlayerUpdateStats(stack, player)) return;

        incrementStatInt(stack, STAT_LOGS_STRIPPED, 1);
        refreshLore(stack);
    }

    public static boolean hasTrackedBy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComp == null) return false;

        CompoundTag customData = nbtComp.copyTag();
        String trackedBy = customData.getString(NBT_STATTRACK_TRACKED_BY_UUID).orElse(null);
        return trackedBy != null && !trackedBy.isBlank();
    }

    public static void onBlocksFlown(ServerPlayer player, ItemStack elytra, int blocks) {
        if (!hasLore(elytra)) return;
        if (!canPlayerUpdateStats(elytra, player)) return;
        if (blocks <= 0) return;

        incrementStatInt(elytra, STAT_BLOCKS_FLOWN, blocks);
        refreshLore(elytra);
    }

    public static void clearSelectedTrackedStat(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComp == null) return;

        CompoundTag customData = nbtComp.copyTag();
        customData.remove(NBT_STATTRACK_SELECTED);

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
    }

    public static void removeStatTrackFromItem(ItemStack stack, boolean clearStoredStats) {
        if (stack == null || stack.isEmpty()) return;

        // Remove our lore line(s)
        net.minecraft.world.item.component.ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null && lore.lines() != null && !lore.lines().isEmpty()) {
            List<Component> kept = new ArrayList<>();
            for (Component line : lore.lines()) {
                if (line == null) continue;
                String s = line.getString();
                if (s != null && s.startsWith(UID_PREFIX)) {
                    continue; // drop "Stat Track (Tracked by ...)" base line
                }
                kept.add(line);
            }

            if (kept.isEmpty()) {
                stack.remove(DataComponents.LORE);
            } else {
                stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(kept));
            }
        }

        // Remove our NBT keys
        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComp != null) {
            CompoundTag customData = nbtComp.copyTag();

            customData.remove(NBT_STATTRACK_SELECTED);
            customData.remove(NBT_STATTRACK_TRACKED_BY_UUID);

            if (clearStoredStats) {
                customData.remove(NBT_STATTRACK_STATS);
                // Optional extras you added (safe to remove if present)
                customData.remove("player_kill_stats");
            }

            // If customData becomes empty, you can remove the component entirely
            if (customData.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
            }
        }
    }

    public static void removeStatTrackAndAllLore(ItemStack stack, boolean clearStoredStats) {
        if (stack == null || stack.isEmpty()) return;

        // Remove ALL lore lines
        stack.remove(DataComponents.LORE);

        // Remove StatTrack-related NBT keys
        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComp != null) {
            CompoundTag customData = nbtComp.copyTag();

            customData.remove(NBT_STATTRACK_SELECTED);
            customData.remove(NBT_STATTRACK_TRACKED_BY_UUID);

            if (clearStoredStats) {
                customData.remove(NBT_STATTRACK_STATS);
                customData.remove("player_kill_stats");
                // optionally also clear any other stat compounds if you want:
                // customData.remove("kill_stats");
                // customData.remove("mining_stats");
                // customData.remove("armor_stats");
            }

            if (customData.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
            }
        }
    }

    public static boolean isTrackedBy(ItemStack stack, ServerPlayer player) {
        if (stack == null || stack.isEmpty() || player == null) return false;

        CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComp == null) return false;

        CompoundTag customData = nbtComp.copyTag();
        String trackedBy = customData.getString(NBT_STATTRACK_TRACKED_BY_UUID).orElse(null);
        if (trackedBy == null || trackedBy.isBlank()) return false;

        return trackedBy.equals(player.getUUID().toString());
    }

    public static boolean hasActiveSelection(ItemStack stack) {
        String sel = getSelectedTrackedStat(stack);
        return sel != null && !sel.isBlank();
    }

    public static int getMaxTracksForPlayer(ServerPlayer player) {
        // Default limit if nothing is granted
        int fallback = 1;

        // Scan from high to low so higher limits win.
        // Adjust 200 if you want a different max supported limit.
        for (int i = 200; i >= 0; i--) {
            if (Permissions.check(player, "stattrack.limit." + i, false)) {
                return i;
            }
        }
        return fallback;
    }
}
