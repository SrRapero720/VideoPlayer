package com.github.NGoedix.watchvideo.util.displayers;

import com.github.NGoedix.watchvideo.util.cache.TextureCache;
import com.github.NGoedix.watchvideo.util.math.geo.Vec3d;
import com.github.NGoedix.watchvideo.util.math.VideoMathUtil;
import org.watermedia.api.math.MathAPI;
import org.watermedia.api.player.videolan.BasePlayer;
import org.watermedia.api.player.videolan.MusicPlayer;
import org.watermedia.api.player.videolan.VideoPlayer;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VideoDisplayer implements IDisplay {

    private static final URI VLC_FAILED = URI.create("https://i.imgur.com/XCcN2uX.png");

    private static final int ACCEPTABLE_SYNC_TIME = 1500;

    private static final List<VideoDisplayer> OPEN_DISPLAYS = new ArrayList<>();

    private boolean stream = false;

    public static void tick() {
        synchronized (OPEN_DISPLAYS) {
            OPEN_DISPLAYS.forEach(VideoDisplayer::pauseIfNecessary);
        }
    }

    private static void pauseIfNecessary(VideoDisplayer display) {
        if (Minecraft.getInstance().isPaused() && display.player.isPlaying() && (display.player.isLive() || display.player.getDuration() > 0)) {
            display.player.setPauseMode(true);
        }
    }

    public static void unload() {
        synchronized (OPEN_DISPLAYS) {
            OPEN_DISPLAYS.forEach(VideoDisplayer::free);
            OPEN_DISPLAYS.clear();
        }
    }

    public static IDisplay createVideoDisplay(Vec3d pos, URI url, float volume, float minDistance, float maxDistance, boolean loop, boolean playing, boolean isOnlyMusic) {
        TextureCache cache = TextureCache.get(VLC_FAILED);

        try {
            final VideoDisplayer display = new VideoDisplayer(pos, url, volume, minDistance, maxDistance, loop, isOnlyMusic);
            if (display.player.isBroken()) throw new IllegalStateException("VideoDisplayer uses a broken player");
            OPEN_DISPLAYS.add(display);
            return display;
        } catch (Exception e) {
            return cache.ready() ? new ImageDisplayer(cache.getPicture()) : null;
        }
    }

    public BasePlayer player;

    private final Vec3d pos;
    private URI url;
    private float lastSetVolume;
    private long lastCorrectedTime = Long.MIN_VALUE;

    public VideoDisplayer(Vec3d pos, URI url, float volume, float minDistance, float maxDistance, boolean loop, boolean isOnlyMusic) {
        this.pos = pos;
        this.url = url;

        if (url == null) {
            if (isOnlyMusic) {
                player = new MusicPlayer();
            } else {
                player = new VideoPlayer(null, Minecraft.getInstance());
            }
            adjustVolume(volume, minDistance, maxDistance);
            player.setRepeatMode(loop);
            player.start(url);
        }
    }

    private void adjustVolume(float volume, float minDistance, float maxDistance) {
        volume = pos != null ? calculateVolume(volume, minDistance, maxDistance) : volume;
        player.setVolume((int) volume);
        lastSetVolume = volume;
    }

    private int calculateVolume(float volume, float minDistance, float maxDistance) {
        if (player == null) return 0;
        Minecraft mc = Minecraft.getInstance();
        float distance = (float) pos.distance(Objects.requireNonNull(Minecraft.getInstance().player).getPosition(mc.isPaused() ? 1.0F : mc.getFrameTime()));
        volume = VideoMathUtil.calculateVolume(volume, distance, minDistance, maxDistance);
        return (int) volume;
    }

    @Override
    public URI getUrl() {
        return url;
    }

    @Override
    public void tick(URI url, float volume, float minDistance, float maxDistance, boolean playing, boolean loop, int tick) {
        if (player == null || url == null)
            return;

        this.url = url;
        volume = pos != null ? calculateVolume(volume, minDistance, maxDistance) : volume;
        if (volume != lastSetVolume) {
            player.setVolume((int) volume);
            lastSetVolume = volume;
        }

        if (player.isSafeUse() && player.isValid()) {
            if (!stream && player.isLive()) stream = true;

            boolean currentPlaying = playing && !Minecraft.getInstance().isPaused();

            player.setPauseMode(!currentPlaying);
            if (!stream && player.isSeekAble()) {
                long time = MathAPI.tickToMs(tick);
                if (time > player.getTime()) time = floorMod(time, player.getMediaInfoDuration());

                if (Math.abs(time - player.getTime()) > ACCEPTABLE_SYNC_TIME && Math.abs(time - lastCorrectedTime) > ACCEPTABLE_SYNC_TIME) {
                    lastCorrectedTime = time;
                    player.seekTo(time);
                }
            }
        }
    }

    public static long floorMod(long x, long y) {
        try {
            final long r = x % y;
            if ((x ^ y) < 0 && r != 0)
                return r + y;
            return r;
        } catch (ArithmeticException e) {
            return 0;
        }
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying() || player.isPaused();
    }

    @Override
    public boolean isStopped() {
        return player.isStopped() || player.isEnded();
    }

    @Override
    public int maxTick() {
        return IDisplay.super.maxTick();
    }

    @Override
    public int prepare(URI url, boolean playing, boolean loop, int tick) {
        if (this.player == null) return -1;
        this.url = url;
        if (this.player instanceof VideoPlayer videoPlayer)
            return videoPlayer.preRender();

        return 0;
    }

    @Override
    public int getRenderTexture() {
        if (this.player instanceof final VideoPlayer videoPlayer)
            return videoPlayer.texture();

        return 0;
    }

    public void free() {
        if (this.player != null) {
            this.player.release();
            this.player = null;
        }
    }

    @Override
    public void release() {
        free();
        synchronized (OPEN_DISPLAYS) {
            OPEN_DISPLAYS.remove(this);
        }
    }

    @Override
    public void stop() {
        if (player == null) return;
        player.stop();
    }

    @Override
    public void pause(int tick) {
        if (player == null) return;
        if (tick != -1)
            player.seekTo(tick);
        player.pause();
    }

    @Override
    public void resume(int tick) {
        if (player == null) return;
        if (tick != -1)
            player.seekTo(tick);
        player.play();
    }

    @Override
    public Dimension getDimensions() {
        if (this.player == null) return null;
        if (this.player instanceof VideoPlayer videoPlayer)
            return videoPlayer.dimension();

        return null;
    }
}
