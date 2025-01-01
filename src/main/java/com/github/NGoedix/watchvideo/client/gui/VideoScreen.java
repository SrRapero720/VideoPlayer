package com.github.NGoedix.watchvideo.client.gui;

import com.github.NGoedix.watchvideo.Reference;
import com.github.NGoedix.watchvideo.VideoPlayerMod;
import com.github.NGoedix.watchvideo.util.math.VideoMathUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.image.ImageRenderer;
import org.watermedia.api.math.MathAPI;
import org.watermedia.api.player.videolan.VideoPlayer;

import java.awt.*;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

public class VideoScreen extends AbstractContainerScreen<AbstractContainerMenu> {

    private static final DateFormat FORMAT = new SimpleDateFormat("HH:mm:ss");
    static {
        FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-00:00"));
    }

    // STATUS
    private int tick = 0;
    private int closingOnTick = -1;
    private float fadeLevel = 0;
    private float fadeStep10 = 0;
    private float fadeStep5 = 0;
    private boolean started;
    private boolean closing = false;
    private float volume;

    // CONTROL
    private final boolean controlBlocked;
    private final boolean canSkip;
    private int optionInMode;
    private int optionInSecs;
    private int optionOutMode;
    private int optionOutSecs;

    // TOOLS
    private final VideoPlayer player;

    // VIDEO INFO
    int videoTexture = -1;

    public VideoScreen(URI url, int volume, boolean controlBlocked, boolean canSkip, int optionInMode, int optionInSecs, int optionOutMode, int optionOutSecs) {
        this(url, volume, controlBlocked, canSkip, optionInMode != -1 && optionInSecs > 0);
        this.optionInMode = optionInMode;
        this.optionInSecs = optionInSecs;
        this.optionOutMode = optionOutMode;
        this.optionOutSecs = optionOutSecs;
    }

    public VideoScreen(URI url, int volume, boolean controlBlocked, boolean canSkip, boolean fadeIn) {
        super(new DummyContainer(), Objects.requireNonNull(Minecraft.getInstance().player).getInventory(), new TextComponent(""));

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().pause();

        this.volume = volume;
        this.controlBlocked = controlBlocked;
        this.canSkip = canSkip;
        this.optionInMode = -1;
        this.optionInSecs = -1;
        this.optionOutMode = -1;
        this.optionOutSecs = -1;

        this.player = new VideoPlayer(null, minecraft);
        Reference.LOGGER.info("Playing video (" + (!controlBlocked ? "not" : "") + "blocked) (" + url + " with volume: " + (int) (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * volume));

        player.setVolume((int) (minecraft.options.getSoundSourceVolume(SoundSource.MASTER) * volume));
        if (!fadeIn) {
            started = true;
            player.start(url);
        } else {
            player.startPaused(url);
        }
    }

    @Override
    protected void renderLabels(@NotNull PoseStack pPoseStack, int pMouseX, int pMouseY) {}

    @Override
    protected void renderBg(@NotNull PoseStack stack, float pPartialTicks, int pMouseX, int pMouseY) {
        if (started && !closing) {
            videoTexture = player.preRender();
        }

        // Handle easing for fade-in
        if ((tick < optionInSecs * 20 && optionInMode != -1) || !started) {
            float t = tick / (float) (optionInSecs * 20);
            fadeLevel = (float) applyEasing(optionInMode, 0, 1, t);
            if (!started && fadeLevel >= 1.0) {
                player.play();
                started = true;
                fadeLevel = 0;
            }
        }

        // Handle easing for fade-out
        if (closing || player.isEnded() || player.isBroken()) {
            if (optionOutMode == -1) {
                System.out.println("Closing without fading out");
                onClose();
            }
            if (optionInMode != -1 || closing) {
                closing = true;
                if (closingOnTick == -1) closingOnTick = tick + optionOutSecs * 20;
                float t = (tick - closingOnTick + optionOutSecs * 20) / (float)(optionOutSecs * 20);
                fadeLevel = (float) applyEasing(optionOutMode, 1, 0, t);
                renderBlackBackground(stack);
                if (fadeLevel == 0) onClose();
                return;
            }
        }

        // BLACK SCREEN
        if (!player.isPaused() || optionInMode != -1 || optionOutMode != -1)
            renderBlackBackground(stack);

        if (!started) return;

        boolean playingState = (player.isPlaying() || player.isPaused());

        // RENDER VIDEO
        if (playingState || player.isStopped() || player.isEnded()) {
            renderTexture(stack, videoTexture);
        }

        // BLACK SCREEN
        if (!player.isPaused())
            renderBlackBackground(stack);

        // RENDER GIF
        if (!player.isPlaying() || !player.isPlaying()) {
            if (player.isPaused() && player.isPaused()) {
                renderIcon(stack, VideoPlayerMod.pausedImage());
            } else {
                renderIcon(stack, ImageAPI.loadingGif());
            }
        }

        // Render icons 10 and -5 seconds
        renderStepIcon(stack, pPartialTicks, true);
        renderStepIcon(stack, pPartialTicks, false);

        // DEBUG RENDERING
        if (!FMLLoader.isProduction()) {
            // TODO: remove state
            draw(stack, String.format("State: %s", player.getStateName()), getHeightCenter(-12));
            draw(stack, String.format("Time: %s (%s) / %s (%s)", FORMAT.format(new Date(player.getTime())), player.getTime(), FORMAT.format(new Date(player.getDuration())), player.getDuration()), getHeightCenter(0));
            draw(stack, String.format("Media Duration: %s (%s)", FORMAT.format(new Date(player.getMediaInfoDuration())), player.getMediaInfoDuration()), getHeightCenter(12));
        }
    }

    private void renderTexture(PoseStack stack, int texture) {
        if (player.dimension() == null) return; // Checking if video available

        RenderSystem.enableBlend();
        fill(stack, 0, 0, width, height, MathAPI.argb(255, 0, 0, 0));
        RenderSystem.disableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);

        // Get video dimensions
        Dimension videoDimensions = player.dimension();
        double videoWidth = videoDimensions.getWidth();
        double videoHeight = videoDimensions.getHeight();

        // Calculate aspect ratios for both the screen and the video
        float screenAspectRatio = (float) imageWidth / imageHeight;
        float videoAspectRatio = (float) ((float) videoWidth / videoHeight);

        // New dimensions for rendering video texture
        int renderWidth, renderHeight;

        // If video's aspect ratio is greater than screen's, it means video's width needs to be scaled down to screen's width
        if(videoAspectRatio > screenAspectRatio) {
            renderWidth = imageWidth;
            renderHeight = (int) (imageWidth / videoAspectRatio);
        } else {
            renderWidth = (int) (imageHeight * videoAspectRatio);
            renderHeight = imageHeight;
        }

        int xOffset = (imageWidth - renderWidth) / 2; // xOffset for centering the video
        int yOffset = (imageHeight - renderHeight) / 2; // yOffset for centering the video

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GuiComponent.blit(stack, xOffset, yOffset, 0.0F, 0.0F, renderWidth, renderHeight, renderWidth, renderHeight);
        RenderSystem.disableBlend();
    }


    private void renderBlackBackground(PoseStack stack) {
        RenderSystem.enableBlend();
        fill(stack, 0, 0, width, height, MathAPI.argb((int) (fadeLevel * 255), 0, 0, 0));
        RenderSystem.disableBlend();
    }

    private int getHeightCenter(int offset) {
        return (height / 2) + offset;
    }

    private void renderIcon(PoseStack stack, ImageRenderer image) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, image.texture(tick, 1, true));

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GuiComponent.blit(stack, width - 36, height - 36 , 0, 0, 36, 36, 28, 28);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        RenderSystem.disableBlend();
    }

    private void renderStepIcon(PoseStack stack, float pPartialTicks, boolean forward) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.bindTexture(forward ? VideoPlayerMod.step10Image().texture(tick, 1, true) : VideoPlayerMod.step5Image().texture(tick, 1, true));
        float alpha = forward ? fadeStep10 : fadeStep5;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        GuiComponent.blit(stack, width / 2 + (forward ? 70 : -134), height / 2 - 32, 0, 0, 64, 64, 64, 64);
        if (forward) {
            fadeStep10 = Math.max(fadeStep10 - (pPartialTicks / 8), 0.0f);
        } else {
            fadeStep5 = Math.max(fadeStep5 - (pPartialTicks / 8), 0.0f);
        }
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        RenderSystem.disableBlend();
    }

    private double applyEasing(int mode, double start, double end, double t) {
        return switch (mode) {
            case 0 -> VideoMathUtil.easeIn(start, end, t);
            case 1 -> VideoMathUtil.easeOut(start, end, t);
            default -> end;
        };
    }

    private void draw(PoseStack stack, String text, int height) {
        drawString(stack, Minecraft.getInstance().font, text, 5, height, 0xffffff);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        // Shift + ESC (Exit)
        if (canSkip && hasShiftDown() && pKeyCode == 256) {
            this.onClose();
        }

        // Up arrow key (Volume)
        if (pKeyCode == 265) {
            if (volume <= 120) {
                volume += 5;
            } else {
                volume = 125;
                float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
                Minecraft.getInstance().options.setSoundCategoryVolume(SoundSource.MASTER, masterVolume <= 0.95 ? masterVolume + 0.1F : 1.0F);
            }

            float actualVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
            float newVolume = volume * actualVolume;
            Reference.LOGGER.info("Volume UP to: " + newVolume);
            player.setVolume((int) newVolume);
        }

        // Down arrow key (Volume)
        if (pKeyCode == 264) {
            if (volume >= 5) {
                volume -= 5;
            } else {
                volume = 0;
            }
            float actualVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
            float newVolume = volume * actualVolume;
            Reference.LOGGER.info("Volume DOWN to: " + newVolume);
            player.setVolume((int) newVolume);
        }

        // M to mute
        if (pKeyCode == 77) {
            if (player.isMuted()) {
                player.unmute();
            } else {
                player.mute();
            }
        }

        // If control blocked can't modify the video time
        if (controlBlocked) return super.keyPressed(pKeyCode, pScanCode, pModifiers);

        // Shift + Right arrow key (Forwards)
        if (hasShiftDown() && pKeyCode == 262) {
            player.seekTo(player.getTime() + 10000);
            fadeStep10 = 1;
        }

        // Shift + Left arrow key (Backwards)
        if (hasShiftDown() && pKeyCode == 263) {
            player.seekTo(player.getTime() - 5000);
            fadeStep5 = 1;
        }

        // Shift + Space (Pause / Play)
        if (hasShiftDown() && pKeyCode == 32) {
            player.togglePlayback();
        }

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (started) {
            started = false;
            player.stop();
            Minecraft.getInstance().getSoundManager().resume();
            GlStateManager._deleteTexture(videoTexture);
            player.release();
        }
    }

    @Override
    protected void init() {
        if (Minecraft.getInstance().screen != null) {
            this.imageWidth = Minecraft.getInstance().screen.width;
            this.imageHeight = Minecraft.getInstance().screen.height;
        }
        super.init();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tick++;
    }
}

