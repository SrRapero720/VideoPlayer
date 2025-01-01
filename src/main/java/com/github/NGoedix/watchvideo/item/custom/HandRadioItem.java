package com.github.NGoedix.watchvideo.item.custom;

import com.github.NGoedix.watchvideo.block.ModBlocks;
import com.github.NGoedix.watchvideo.network.PacketHandler;
import com.github.NGoedix.watchvideo.network.message.OpenRadioManagerScreen;
import com.github.NGoedix.watchvideo.util.cache.TextureCache;
import com.github.NGoedix.watchvideo.util.config.TVConfig;
import com.github.NGoedix.watchvideo.util.displayers.IDisplay;
import com.github.NGoedix.watchvideo.util.math.geo.Vec3d;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HandRadioItem extends BlockItem {

    private static final Map<ItemStack, IDisplay> displays = new ConcurrentHashMap<>();
    private static final Map<ItemStack, TextureCache> caches = new ConcurrentHashMap<>();

    public HandRadioItem(Properties pProperties) {
        super(ModBlocks.HAND_RADIO_BLOCK.get(), pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        if (!pLevel.isClientSide) {
            ItemStack stack = pPlayer.getItemInHand(pHand);
            CompoundTag tag = stack.getOrCreateTag();

            String url = tag.getString("url");
            int volume = tag.getInt("volume");
            boolean isPlaying = tag.getBoolean("isPlaying");

            PacketHandler.sendTo(new OpenRadioManagerScreen(pPlayer.getItemInHand(pHand), url, volume, isPlaying), (ServerPlayer) pPlayer);
        }

        return InteractionResultHolder.success(pPlayer.getItemInHand(pHand));
    }

    @Nullable
    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null) {
            nbt = new CompoundTag();
        }
        return nbt;
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt) {
        if (nbt != null) {
            stack.setTag(nbt);
        }
    }

    public static void setUrl(ItemStack stack, String url) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("url", url);
    }

    public static URI getUrl(ItemStack stack) {
        final CompoundTag tag = stack.getOrCreateTag();
        final String url = tag.getString("url");
        if (url.isEmpty()) return null;

        try {
            return new URI(url);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setVolume(ItemStack stack, int volume) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("volume", volume);
    }

    public static int getVolume(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt("volume");
    }

    public static void setIsPlaying(ItemStack stack, boolean isPlaying) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("isPlaying", isPlaying);
    }

    public static boolean getIsPlaying(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getBoolean("isPlaying");
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity pEntity, int pSlotId, boolean pIsSelected) {
        if (level.isClientSide) {
            IDisplay display = requestDisplay(stack);
            if (display != null) {
                display.tick(getUrl(stack), getVolume(stack), TVConfig.MIN_DISTANCE, TVConfig.MAX_DISTANCE, getIsPlaying(stack), true, 0);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static IDisplay requestDisplay(ItemStack stack) {
        final URI url = getUrl(stack);
        if (url == null) return null;

        final TextureCache cache = caches.computeIfAbsent(stack, s -> TextureCache.get(url));
        if (!url.equals(cache.url)) {
            final TextureCache newCache = TextureCache.get(url);
            caches.put(stack, newCache);
            IDisplay oldDisplay = displays.remove(stack);
            if (oldDisplay != null) {
                oldDisplay.release();
            }
        }

        if (!cache.isVideo() && (!cache.ready() || cache.getError() != null)) {
            return null;
        }

        return displays.computeIfAbsent(stack, s -> cache.createDisplay(new Vec3d(0, 0, 0), url, getVolume(stack), TVConfig.MIN_DISTANCE, TVConfig.MAX_DISTANCE, true, getIsPlaying(stack), true));
    }

    private static boolean isURLEmpty(String url) {
        return url == null || url.isEmpty();
    }
}