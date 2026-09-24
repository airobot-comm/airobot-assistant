package com.airobot.agent.skills.volume

/**
 * Capability interface for setting system audio volume.
 */
interface VolumeProvider {
    /**
     * Sets speaker volume level (0 to 100).
     */
    fun setVolume(volume: Int)
}
