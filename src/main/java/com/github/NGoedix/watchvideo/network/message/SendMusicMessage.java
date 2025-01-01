package com.github.NGoedix.watchvideo.network.message;

import com.github.NGoedix.watchvideo.client.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class SendMusicMessage implements IMessage<SendMusicMessage> {

    private String url;
    private int volume;
    private final MusicMessageType state;

    public SendMusicMessage() {
        this.state = MusicMessageType.STOP;
    }

    public SendMusicMessage(URI url, int volume) {
        this.url = url.toString();
        this.volume = volume;
        this.state = MusicMessageType.START;
    }

    public SendMusicMessage(String url, int volume) {
        this.url = url;
        this.volume = volume;
        this.state = MusicMessageType.START;
    }

    @Override
    public void encode(SendMusicMessage message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.state);
        if (message.state == MusicMessageType.START) {
            buffer.writeUtf(message.url);
            buffer.writeInt(message.volume);
        }
    }

    @Override
    public SendMusicMessage decode(FriendlyByteBuf buffer) {
        MusicMessageType state = buffer.readEnum(MusicMessageType.class);
        if (state == MusicMessageType.START) {
            String url = buffer.readUtf();
            int volume = buffer.readInt();
            return new SendMusicMessage(URI.create(url), volume);
        }
        return new SendMusicMessage();
    }

    @Override
    public void handle(SendMusicMessage message, Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            if (message.state == MusicMessageType.START) ClientHandler.playMusic(message.url == null || message.url.isEmpty() ? null : URI.create(message.url), message.volume);
            if (message.state == MusicMessageType.STOP) ClientHandler.stopMusicIfPlaying();
        });
        supplier.get().setPacketHandled(true);
    }

    enum MusicMessageType {
        START,
        STOP
    }
}
