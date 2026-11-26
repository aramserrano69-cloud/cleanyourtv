package com.gathin.tvhome;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

/** Fondo de video en bucle y sin audio (res/raw/fondo.mp4). Se libera cuando Home no se ve. */
public class VideoBackground extends TextureView implements TextureView.SurfaceTextureListener {
    private MediaPlayer player;
    private Surface surface;
    private int videoWidth, videoHeight;

    public VideoBackground(Context context) {
        this(context, null);
    }

    public VideoBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSurfaceTextureListener(this);
        setOpaque(true);
    }

    private void play() {
        if (surface == null || player != null || getWindowVisibility() != View.VISIBLE) return;
        try {
            MediaPlayer mp = new MediaPlayer();
            player = mp;
            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.fondo);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setSurface(surface);
            mp.setLooping(true);
            mp.setVolume(0f, 0f);
            mp.setOnVideoSizeChangedListener((m, w, h) -> {
                videoWidth = w;
                videoHeight = h;
                fit();
            });
            mp.setOnPreparedListener(m -> {
                if (player == m && getWindowVisibility() == View.VISIBLE) m.start();
            });
            mp.setOnErrorListener((m, what, extra) -> {
                stop();
                setVisibility(View.GONE);
                return true;
            });
            mp.prepareAsync();
        } catch (Exception e) {
            stop();
            setVisibility(View.GONE);
        }
    }

    private void stop() {
        MediaPlayer mp = player;
        player = null;
        if (mp != null) {
            try {
                mp.release();
            } catch (Exception ignored) {
            }
        }
    }

    /** Recorta el video para cubrir toda la pantalla, sin deformarlo. */
    private void fit() {
        float vw = getWidth(), vh = getHeight();
        if (videoWidth <= 0 || videoHeight <= 0 || vw <= 0 || vh <= 0) return;
        float scale = Math.max(vw / videoWidth, vh / videoHeight);
        Matrix m = new Matrix();
        m.setScale(videoWidth * scale / vw, videoHeight * scale / vh, vw / 2f, vh / 2f);
        setTransform(m);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) play();
        else stop();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
        surface = new Surface(st);
        play();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
        fit();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
        stop();
        if (surface != null) surface.release();
        surface = null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture st) {
    }
}
