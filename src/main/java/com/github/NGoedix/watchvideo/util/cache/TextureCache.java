package com.github.NGoedix.watchvideo.util.cache;

import com.github.NGoedix.watchvideo.util.displayers.IDisplay;
import com.github.NGoedix.watchvideo.util.displayers.ImageDisplayer;
import com.github.NGoedix.watchvideo.util.displayers.VideoDisplayer;
import com.github.NGoedix.watchvideo.util.math.geo.Vec3d;
import org.watermedia.api.image.ImageFetch;
import org.watermedia.api.image.ImageRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TextureCache {
    private static final Map<URI, TextureCache> CACHE = new HashMap<>();

    public final URI url;
    private volatile ImageFetch seeker;
    private volatile ImageRenderer picture;
    private volatile String error;
    private volatile boolean ready = false;
    private volatile boolean isVideo = false;
    private final AtomicInteger usage = new AtomicInteger();

    private TextureCache(URI url) {
        this.url = url;
        use();
        attemptToLoad();
    }

    public static TextureCache get(URI url) {
        TextureCache cache = CACHE.get(url);
        if (cache != null) {
            cache.use();
            return cache;
        }
        cache = new TextureCache(url);
        CACHE.put(url, cache);
        return cache;
    }

    public ImageRenderer getPicture() {
        return picture;
    }

    private synchronized void attemptToLoad() {
        if (this.seeker != null) return;
        if (this.url != null) {
            this.seeker = new ImageFetch(url);
            this.seeker.setSuccessCallback((imageRenderer, fromCache) ->
                    Minecraft.getInstance().executeBlocking(() -> this.process(imageRenderer))
            );
            this.seeker.setErrorCallback((e, isVideo) -> Minecraft.getInstance().executeBlocking(() -> {

                if (isVideo) {
                    this.processVideo();
                    return;
                }

                if (e == null) this.processFailed("download.exception.gif");
                else if (e.getMessage().startsWith("Server returned HTTP response code: 403")) this.processFailed("download.exception.forbidden");
                else if (e.getMessage().startsWith("Server returned HTTP response code: 404")) this.processFailed("download.exception.notfound");
                else this.processFailed("download.exception.invalid");
            }));
            this.seeker.start();
        }
    }

    public IDisplay createDisplay(Vec3d pos, URI url, float volume, float minDistance, float maxDistance, boolean loop, boolean playing, boolean isOnlyMusic) {
        volume *= Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
        if (picture == null) return VideoDisplayer.createVideoDisplay(pos, url, volume, minDistance, maxDistance, loop, playing, isOnlyMusic);

        return new ImageDisplayer(picture) {
            @Override
            public void release() { deuse(); }
        };
    }

    public void process(ImageRenderer picture) {
        if (ready) return;
        this.ready = true;
        this.seeker = null;
        this.picture = picture;
        this.error = null;
        this.isVideo = false;
    }

    public void processVideo() {
        this.ready = true;
        this.seeker = null;
        this.picture = null;
        this.error = null;
        this.isVideo = true;
    }

    public void processFailed(String error) {
        this.error = error;
        this.seeker = null;
        this.picture = null;
        this.ready = true;
        this.isVideo = false;
    }

    public synchronized boolean ready() {
        if (ready || seeker == null) return true;
        attemptToLoad();
        return false;
    }

    public String getError() { return error; }
    public boolean isVideo() { return isVideo; }
    public void use() { usage.incrementAndGet(); }
    public void deuse() { usage.decrementAndGet(); }
    public boolean isUsed() { return usage.get() > 0; }

    public void remove() {
        ready = false;
        if (picture != null) {
            picture.release();
            Arrays.fill(picture.textures, -1);
        }
        picture = null;
        seeker = null;
    }

    public static void clientTick() { CACHE.values().removeIf(o -> !o.isUsed()); }
    public static void renderTick() { VideoDisplayer.tick(); }
    public static void unload() { for (TextureCache cache : CACHE.values()) cache.remove(); CACHE.clear(); }
}
