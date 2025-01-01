package com.github.NGoedix.watchvideo.network.message;

import com.github.NGoedix.watchvideo.Reference;
import com.github.NGoedix.watchvideo.block.entity.custom.TVBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.net.URI;
import java.util.UUID;
import java.util.function.Supplier;

public class UploadVideoUpdateMessage implements IMessage<UploadVideoUpdateMessage> {

    private BlockPos blockPos;
    private String url;
    private int volume;
    private int tick;
    private boolean isPlaying;
    private boolean stopped;
    private boolean exit;

    public UploadVideoUpdateMessage() {}

    public UploadVideoUpdateMessage(BlockPos blockPos, URI url, int volume, int tick, boolean isPlaying, boolean stopped, boolean exit) {
        this.blockPos = blockPos;
        this.url = url == null ? "" : url.toString();
        this.volume = volume;
        this.tick = tick;
        this.isPlaying = isPlaying;
        this.stopped = stopped;
        this.exit = exit;
    }

    public UploadVideoUpdateMessage(BlockPos blockPos, String url, int volume, int tick, boolean isPlaying, boolean stopped, boolean exit) {
        this.blockPos = blockPos;
        this.url = url;
        this.volume = volume;
        this.tick = tick;
        this.isPlaying = isPlaying;
        this.stopped = stopped;
        this.exit = exit;
    }

    @Override
    public void encode(UploadVideoUpdateMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.blockPos);
        buffer.writeUtf(message.url);
        buffer.writeInt(message.volume);
        buffer.writeInt(message.tick);
        buffer.writeBoolean(message.isPlaying);
        buffer.writeBoolean(message.stopped);
        buffer.writeBoolean(message.exit);
    }

    @Override
    public UploadVideoUpdateMessage decode(FriendlyByteBuf buffer) {
        return new UploadVideoUpdateMessage(buffer.readBlockPos(), buffer.readUtf(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    @Override
    public void handle(UploadVideoUpdateMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player == null) return;
            if (player.level.getBlockEntity(message.blockPos) instanceof TVBlockEntity) {
                if (player.level.getBlockEntity(message.blockPos) instanceof TVBlockEntity tv) {
                    if (message.exit)
                        tv.setBeingUsed(new UUID(0, 0));
                    else {
                        tv.setUrl(this.url == null || this.url.isEmpty() ? null : URI.create(message.url));
                        tv.setVolume(message.volume);

                        if (message.tick != -1)
                            tv.setTick(message.tick);

                        tv.setPlaying(message.isPlaying);

                        if (message.stopped)
                            tv.stop();

                        tv.notifyPlayer();
                    }}
                } else {
                Reference.LOGGER.info("BlockEntity is not a TV");
            }

        });
        ctx.get().setPacketHandled(true);
    }
}
