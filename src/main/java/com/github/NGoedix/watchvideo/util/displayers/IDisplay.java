package com.github.NGoedix.watchvideo.util.displayers;

import java.awt.*;
import java.net.URI;

public interface IDisplay {

    URI getUrl();

    int prepare(URI url, boolean playing, boolean loop, int tick);

    void tick(URI url, float volume, float minDistance, float maxDistance, boolean playing, boolean loop, int tick);

    default int maxTick() {
        return 0;
    }

    void pause(int tick);

    void resume(int tick);

    int getRenderTexture();

    boolean isPlaying();

    boolean isStopped();

    void release();

    void stop();

    Dimension getDimensions();
}
