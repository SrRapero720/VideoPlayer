package com.github.NGoedix.watchvideo.network.message;

import com.github.NGoedix.watchvideo.Reference;
import com.github.NGoedix.watchvideo.client.ClientHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.net.URI;
import java.util.function.Supplier;

public class OpenVideoManagerScreen implements IMessage<OpenVideoManagerScreen> {

    private BlockPos blockPos;
    private String url;
    private int volume;
    private int tick;
    private boolean isPlaying;

    public OpenVideoManagerScreen() {}

    public OpenVideoManagerScreen(BlockPos blockPos, String url, int volume, int tick, boolean isPlaying) {
        this.blockPos = blockPos;
        this.url = url;
        this.volume = volume;
        this.tick = tick;
        this.isPlaying = isPlaying;
    }

    @Override
    public void encode(OpenVideoManagerScreen message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.blockPos);
        buffer.writeUtf(message.url);
        buffer.writeInt(message.volume);
        buffer.writeInt(message.tick);
        buffer.writeBoolean(message.isPlaying);
    }

    @Override
    public OpenVideoManagerScreen decode(FriendlyByteBuf buffer) {
        return new OpenVideoManagerScreen(buffer.readBlockPos(), buffer.readUtf(), buffer.readInt(), buffer.readInt(), buffer.readBoolean());
    }

    @Override
    public void handle(OpenVideoManagerScreen message, Supplier<NetworkEvent.Context> supplier) {
        Reference.LOGGER.info("HANDLE OUT");
        supplier.get().enqueueWork(() -> {
            Reference.LOGGER.info("handle");
            ClientHandler.openVideoGUI(message.blockPos, (message.url == null || message.url.isEmpty()) ? null : URI.create(message.url), message.volume, message.tick, message.isPlaying);
        });
        supplier.get().setPacketHandled(true);
    }
}
