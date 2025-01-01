package com.github.NGoedix.watchvideo.network.message;


import com.github.NGoedix.watchvideo.Reference;
import com.github.NGoedix.watchvideo.block.entity.custom.RadioBlockEntity;
import com.github.NGoedix.watchvideo.item.custom.HandRadioItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.net.URI;
import java.util.UUID;
import java.util.function.Supplier;

public class UploadRadioUpdateMessage implements IMessage<UploadRadioUpdateMessage> {

    private BlockPos blockPos;
    private ItemStack stack;
    private String url;
    private int volume;
    private int tick;
    private boolean isPlaying;
    private boolean exit;

    public UploadRadioUpdateMessage() {}

    public UploadRadioUpdateMessage(BlockPos blockPos, String url, int volume, int tick, boolean isPlaying, boolean exit) {
        this.blockPos = blockPos;
        this.stack = null;
        this.url = url;
        this.volume = volume;
        this.tick = tick;
        this.isPlaying = isPlaying;
        this.exit = exit;
    }

    public UploadRadioUpdateMessage(ItemStack stack, String url, int volume, int tick, boolean isPlaying, boolean exit) {
        this.blockPos = null;
        this.stack = stack;
        this.url = url;
        this.volume = volume;
        this.tick = tick;
        this.isPlaying = isPlaying;
        this.exit = exit;
    }

    @Override
    public void encode(UploadRadioUpdateMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.stack != null); // True if itemstack
        if (message.stack != null)
            buffer.writeItem(message.stack);
        else
            buffer.writeBlockPos(message.blockPos);
        buffer.writeUtf(message.url);
        buffer.writeInt(message.volume);
        buffer.writeInt(message.tick);
        buffer.writeBoolean(message.isPlaying);
        buffer.writeBoolean(message.exit);
    }

    @Override
    public UploadRadioUpdateMessage decode(FriendlyByteBuf buffer) {
        if (buffer.readBoolean())
            return new UploadRadioUpdateMessage(buffer.readItem(), buffer.readUtf(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean());
        else
            return new UploadRadioUpdateMessage(buffer.readBlockPos(), buffer.readUtf(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean());
    }

    @Override
    public void handle(UploadRadioUpdateMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            final Player player = ctx.get().getSender();
            if (player == null) return;
            if (message.blockPos != null) {
                if (player.level.getBlockEntity(message.blockPos) instanceof RadioBlockEntity radioBlock) {
                    if (message.exit)
                        radioBlock.setBeingUsed(new UUID(0, 0));
                    else {
                        radioBlock.setUrl(this.url == null || this.url.isEmpty() ? null : URI.create(message.url));

                        if (tick != -1)
                            radioBlock.setTick(message.tick);

                        radioBlock.setVolume(message.volume);
                        radioBlock.setPlaying(message.isPlaying);

                        radioBlock.notifyPlayer();
                    }
                } else {
                    Reference.LOGGER.info("CLICKED");
                }
            } else {
                HandRadioItem.setUrl(message.stack, message.url);
                HandRadioItem.setVolume(message.stack, message.volume);
                HandRadioItem.setIsPlaying(message.stack, message.isPlaying);

                player.inventoryMenu.broadcastChanges();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
