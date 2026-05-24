package com.rst.outspelled.util;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

public final class SoundManager {

    // SFX clip references (preloaded at initialize() for zero-latency playback)
    private static AudioClip clickClip;
    private static AudioClip castClip;
    private static AudioClip invalidClip;
    private static AudioClip victoryClip;
    private static AudioClip defeatClip;
    private static AudioClip skillCheckClip;
    private static AudioClip keyTapClip;
    private static AudioClip keyDeleteClip;

    // BGM player (streams audio; supports loop)
    private static MediaPlayer bgmPlayer;

    // Volume levels (0.0 to 1.0)
    private static double sfxVolume = 0.5;
    private static double bgmVolume = 0.5;

    // Whether audio is globally muted
    private static boolean muted = false;

    /**
     * Preloads all SFX clips from the audio resource folder.
     * Call once in Main.start() before any screen is shown.
     */
    public static void initialize() {
        clickClip = loadClip("Mouse_Click.wav");
        castClip = loadClip("cast.wav");
        invalidClip = loadClip("Key_delete.wav"); // invalid word reuses the delete sound
        victoryClip = loadClip("VictoryFare.wav");
        defeatClip = loadClip("DefeatSound.wav");
        skillCheckClip = loadClip("Correct_Sound_Effect.wav");
        keyTapClip = loadClip("Key_delete.wav");
        keyDeleteClip = loadClip("Key_delete.wav");

        // Pre-warm the click clip at volume 0 so the audio subsystem is primed
        // and the very first real click has no perceptible delay.
        if (clickClip != null)
            clickClip.play(0.0);
    }

    // -------------------------------------------------------
    // SFX
    // -------------------------------------------------------

    public static void playClick() {
        play(clickClip);
    }

    public static void playCast() {
        play(castClip);
    }

    public static void playKeyTap() {
        play(keyTapClip);
    }

    /** Plays the delete-key sound (also used for invalid word feedback). */
    public static void playKeyDelete() {
        play(keyDeleteClip);
    }

    /** Plays the invalid-word sound (same as delete for now). */
    public static void playInvalid() {
        play(invalidClip);
    }

    /** Plays the victory fanfare (winner). */
    public static void playVictory() {
        play(victoryClip);
    }

    /** Plays the defeat sting (loser). */
    public static void playDefeat() {
        play(defeatClip);
    }

    /** Plays the skill-check correct sound. */
    public static void playSkillCheck() {
        play(skillCheckClip);
    }

    // -------------------------------------------------------
    // BGM
    // -------------------------------------------------------

    /**
     * Starts looping the given background music file, stops any currently playing
     * BGM first.
     * 
     * @param filename filename inside the audio resource folder (e.g.
     *                 "MenuBGM.mp3")
     */
    public static void startBgm(String filename) {
        stopBgm();
        URL url = SoundManager.class.getResource("/com/rst/outspelled/audio/" + filename);
        if (url == null) {
            System.err.println("[SoundManager] BGM file not found: " + filename);
            return;
        }
        try {
            bgmPlayer = new MediaPlayer(new Media(url.toExternalForm()));
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE); // loop forever
            bgmPlayer.setVolume(muted ? 0.0 : bgmVolume);
            bgmPlayer.play();
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to play BGM: " + e.getMessage());
        }
    }

    /** Stops the currently playing background music. */
    public static void stopBgm() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
    }

    /** Pauses the BGM without disposing it. */
    public static void pauseBgm() {
        if (bgmPlayer != null)
            bgmPlayer.pause();
    }

    /** Resumes a paused BGM. */
    public static void resumeBgm() {
        if (bgmPlayer != null && !muted)
            bgmPlayer.play();
    }

    // -------------------------------------------------------
    // Volume controls
    // -------------------------------------------------------

    /**
     * Sets the SFX volume.
     * 
     * @param volume value between 0.0 (silent) and 1.0 (full)
     */
    public static void setSfxVolume(double volume) {
        sfxVolume = Math.max(0.0, Math.min(1.0, volume));
        if (clickClip != null)
            clickClip.setVolume(sfxVolume);
        if (castClip != null)
            castClip.setVolume(sfxVolume);
        if (invalidClip != null)
            invalidClip.setVolume(sfxVolume);
        if (victoryClip != null)
            victoryClip.setVolume(sfxVolume);
        if (defeatClip != null)
            defeatClip.setVolume(sfxVolume);
        if (skillCheckClip != null)
            skillCheckClip.setVolume(sfxVolume);
        if (keyTapClip != null)
            keyTapClip.setVolume(sfxVolume);
        if (keyDeleteClip != null)
            keyDeleteClip.setVolume(sfxVolume);
    }

    /**
     * Sets the BGM volume.
     * 
     * @param volume value between 0.0 (silent) and 1.0 (full)
     */
    public static void setBgmVolume(double volume) {
        bgmVolume = Math.max(0.0, Math.min(1.0, volume));
        if (bgmPlayer != null && !muted)
            bgmPlayer.setVolume(bgmVolume);
    }

    /**
     * Toggles global mute. Muting silences both SFX and BGM
     * without changing the stored volume levels.
     */
    public static void setMuted(boolean mute) {
        muted = mute;
        if (bgmPlayer != null)
            bgmPlayer.setVolume(muted ? 0.0 : bgmVolume);
        // SFX clips are silenced at play-time (checked in play())
    }

    public static boolean isMuted() {
        return muted;
    }

    public static double getSfxVolume() {
        return sfxVolume;
    }

    public static double getBgmVolume() {
        return bgmVolume;
    }

    // -------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------

    // Loads an AudioClip from the audio resource folder, returns null if not found
    private static AudioClip loadClip(String filename) {
        URL url = SoundManager.class.getResource("/com/rst/outspelled/audio/" + filename);
        if (url == null) {
            System.err.println("[SoundManager] SFX file not found: " + filename);
            return null;
        }
        try {
            AudioClip clip = new AudioClip(url.toExternalForm());
            clip.setVolume(sfxVolume);
            return clip;
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to load SFX: " + filename + " — " + e.getMessage());
            return null;
        }
    }

    // Plays a clip if it exists and audio is not muted
    private static void play(AudioClip clip) {
        if (clip != null && !muted)
            clip.play(sfxVolume);
    }

    // Utility class — no instances
    private SoundManager() {
    }
}
