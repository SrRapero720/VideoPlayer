package com.github.NGoedix.watchvideo.block.entity.custom;

import com.github.NGoedix.watchvideo.util.cache.TextureCache;
import com.github.NGoedix.watchvideo.util.config.TVConfig;
import com.github.NGoedix.watchvideo.util.displayers.IDisplay;
import com.github.NGoedix.watchvideo.util.math.geo.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.net.URI;

public abstract class VideoPlayerBlockEntity extends BlockEntity {

    // VIDEO / MUSIC  PROPERTIES
    private URI url = null;
    private boolean playing = false;
    private boolean stopped = false;

    private int volume = 100;
    private int tick = 0;

    private final boolean loop = true;

    @OnlyIn(Dist.CLIENT)
    public IDisplay display;

    @OnlyIn(Dist.CLIENT)
    public TextureCache cache;

    private final boolean isOnlyMusic;

    public VideoPlayerBlockEntity(BlockEntityType<?> tileEntity, BlockPos pWorldPosition, BlockState pBlockState, boolean isOnlyMusic) {
        super(tileEntity, pWorldPosition, pBlockState);
        this.isOnlyMusic = isOnlyMusic;
    }

    public boolean isURLEmpty() {
        return url == null;
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
        if (this.level == null) return;
        this.level.blockEntityChanged(this.worldPosition);
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    public void setVolume(int volume) {
        this.volume = volume;
        if (this.level == null) return;
        this.level.blockEntityChanged(this.worldPosition);
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    public int getVolume() {
        return volume;
    }

    public boolean isPlaying() {
        return playing && !stopped;
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
        if (this.level == null) return;
        this.level.blockEntityChanged(this.worldPosition);
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public void stop() {
        stopped = true;
    }

    public IDisplay requestDisplay() {
        URI url = getUrl();
        if (isURLEmpty()) return null;
        if (cache == null || cache.url == null || !cache.url.equals(url)) {
            cache = TextureCache.get(url);
            if (display != null)
                display.release();
            display = null;
        }
        if (!cache.isVideo() && (!cache.ready() || cache.getError() != null))
            return null;
        if (display != null)
            return display;

        return display = cache.createDisplay(new Vec3d(worldPosition), url, volume, TVConfig.MIN_DISTANCE, TVConfig.MAX_DISTANCE, loop, playing, isOnlyMusic);
    }

    public void tick() {}

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.handleUpdateTag(pkt.getTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag nbt) {
        loadFromNBT(nbt);
        this.level.blockEntityChanged(this.worldPosition);
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithFullMetadata();
    }

    @Override
    public void setRemoved() {
        if (isClient() && display != null)
            display.release();
    }

    @Override
    public void onChunkUnloaded() {
        if (isClient() && display != null)
            display.release();
    }

    public boolean isClient() {
        return this.level != null && this.level.isClientSide;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);

        pTag.putString("url", url == null ? "" : url.toString());
        pTag.putBoolean("playing", playing);
        pTag.putInt("tick", tick);
        pTag.putInt("volume", volume);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);

        loadFromNBT(pTag);
    }

    protected abstract void loadFromNBT(CompoundTag nbt);

    public void loadFromNBTInternal(CompoundTag nbt) {
        loadFromNBT(nbt);
        String str = nbt.getString("url");
        if (str.isEmpty()) this.url = null;

        try {
            this.url = new URI(str);
        } catch (Exception e) {
            this.url = null;
        }

        this.playing = nbt.getBoolean("playing");
        this.tick = nbt.getInt("tick");
        this.volume = nbt.getInt("volume");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        VideoPlayerBlockEntity be = (VideoPlayerBlockEntity) level.getBlockEntity(pos);
        if (level.isClientSide) {
            IDisplay display = be.requestDisplay();
            if (display != null) {
                if (be.stopped)
                    display.stop();
                be.stopped = false;
                display.tick(be.url, be.volume, TVConfig.MIN_DISTANCE, TVConfig.MAX_DISTANCE, be.playing, be.loop, be.isOnlyMusic ? 0 : be.tick);
            }
        }
        if (be == null) return;
        if (be.playing)
            be.tick++;
        be.tick();
    }
}
