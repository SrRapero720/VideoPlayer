package com.github.NGoedix.watchvideo.network.message;

import com.github.NGoedix.watchvideo.client.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class SendVideoMessage implements IMessage<SendVideoMessage> {

    private String url;
    private int volume;
    private boolean isControlBlocked;
    private boolean canSkip;
    private VideoMessageType state;

    public SendVideoMessage() {
        this.state = VideoMessageType.STOP;
    }

    public SendVideoMessage(String url, int volume, boolean isControlBlocked, boolean canSkip) {
        this.url = url;
        this.volume = volume;
        this.isControlBlocked = isControlBlocked;
        this.canSkip = canSkip;
        this.state = VideoMessageType.START;
    }

    @Override
    public void encode(SendVideoMessage message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.state);
        if (message.state == VideoMessageType.START) {
            final String str = message.url.toString();
            buffer.writeUtf(str);
            buffer.writeInt(message.volume);
            buffer.writeBoolean(message.isControlBlocked);
            buffer.writeBoolean(message.canSkip);
        }
    }

    @Override
    public SendVideoMessage decode(FriendlyByteBuf buffer) {
        VideoMessageType state = buffer.readEnum(VideoMessageType.class);
        if (state == VideoMessageType.START) {
            String url = buffer.readUtf();
            int volume = buffer.readInt();
            boolean isControlBlocked = buffer.readBoolean();
            boolean canSkip = buffer.readBoolean();
            return new SendVideoMessage(url, volume, isControlBlocked, canSkip);
        }
        return new SendVideoMessage();
    }

    @Override
    public void handle(SendVideoMessage message, Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            if (message.state == VideoMessageType.START) ClientHandler.openVideo(URI.create(message.url), message.volume, message.isControlBlocked, message.canSkip);
            if (message.state == VideoMessageType.STOP) ClientHandler.stopVideoIfExists();
        });
        supplier.get().setPacketHandled(true);
    }

    enum VideoMessageType {
        START,
        STOP
    }
}
