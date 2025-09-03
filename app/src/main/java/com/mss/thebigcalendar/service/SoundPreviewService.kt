package com.mss.thebigcalendar.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.mss.thebigcalendar.data.model.NotificationSoundType

class SoundPreviewService(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null

    /**
     * Reproduz o som de preview baseado no tipo selecionado
     */
    fun playSoundPreview(soundType: NotificationSoundType) {
        // Parar qualquer som anterior
        stopSoundPreview()

        when (soundType) {
            NotificationSoundType.DEFAULT -> {
                playDefaultNotificationSound()
            }
            NotificationSoundType.BEEP -> {
                playBeepSound()
            }
            NotificationSoundType.CHIME -> {
                playChimeSound()
            }
            NotificationSoundType.BELL -> {
                playBellSound()
            }
            NotificationSoundType.ALERT -> {
                playAlertSound()
            }
            NotificationSoundType.NOTIFICATION -> {
                playNotificationSound()
            }
            NotificationSoundType.RINGTONE -> {
                playRingtoneSound()
            }
            NotificationSoundType.VIBRATION_ONLY -> {
                // Apenas vibração - não reproduz som
                return
            }
        }
    }

    /**
     * Para o som de preview atual
     */
    fun stopSoundPreview() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
                // Ignorar erros ao parar MediaPlayer
            }
        }
        mediaPlayer = null
        
        audioTrack?.let { track ->
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            } catch (e: Exception) {
                // Ignorar erros ao parar AudioTrack
            }
        }
        audioTrack = null
    }

    /**
     * Reproduz o som padrão de notificação do sistema
     */
    private fun playDefaultNotificationSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (notificationUri != null) {
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            }
        } catch (e: Exception) {
            // Fallback para som simples se não conseguir reproduzir o som padrão
            playBeepSound()
        }
    }

    /**
     * Reproduz um som de beep simples
     */
    private fun playBeepSound() {
        try {
            val sampleRate = 44100
            val duration = 0.2f // 200ms
            val frequency = 800
            val samples = (sampleRate * duration).toInt()
            val sound = generateTone(frequency, samples, sampleRate)
            
            playAudioData(sound, sampleRate)
        } catch (e: Exception) {
            // Se falhar, usar o som padrão
            playDefaultNotificationSound()
        }
    }

    /**
     * Reproduz um som de sino
     */
    private fun playChimeSound() {
        try {
            val sampleRate = 44100
            val duration = 0.5f
            val samples = (sampleRate * duration).toInt()
            val sound = generateChime(samples, sampleRate)
            
            playAudioData(sound, sampleRate)
        } catch (e: Exception) {
            playDefaultNotificationSound()
        }
    }

    /**
     * Reproduz um som de sino clássico
     */
    private fun playBellSound() {
        try {
            val sampleRate = 44100
            val duration = 0.8f
            val samples = (sampleRate * duration).toInt()
            val sound = generateBell(samples, sampleRate)
            
            playAudioData(sound, sampleRate)
        } catch (e: Exception) {
            playDefaultNotificationSound()
        }
    }

    /**
     * Reproduz um som de alerta
     */
    private fun playAlertSound() {
        try {
            val sampleRate = 44100
            val duration = 0.3f
            val samples = (sampleRate * duration).toInt()
            val sound = generateAlert(samples, sampleRate)
            
            playAudioData(sound, sampleRate)
        } catch (e: Exception) {
            playDefaultNotificationSound()
        }
    }

    /**
     * Reproduz um som de notificação personalizado
     */
    private fun playNotificationSound() {
        try {
            val sampleRate = 44100
            val duration = 0.4f
            val samples = (sampleRate * duration).toInt()
            val sound = generateNotification(samples, sampleRate)
            
            playAudioData(sound, sampleRate)
        } catch (e: Exception) {
            playDefaultNotificationSound()
        }
    }

    /**
     * Reproduz um som de toque
     */
    private fun playRingtoneSound() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (ringtoneUri != null) {
                val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
                ringtone?.play()
            } else {
                playDefaultNotificationSound()
            }
        } catch (e: Exception) {
            playDefaultNotificationSound()
        }
    }

    /**
     * Gera um tom simples
     */
    private fun generateTone(frequency: Int, samples: Int, sampleRate: Int): ByteArray {
        val sound = ByteArray(samples * 2) // 16-bit
        for (i in 0 until samples) {
            val sample = (Math.sin(2 * Math.PI * i * frequency / sampleRate) * 0.3).toFloat()
            val sampleShort = (sample * Short.MAX_VALUE).toInt()
            sound[i * 2] = (sampleShort and 0xFF).toByte()
            sound[i * 2 + 1] = ((sampleShort shr 8) and 0xFF).toByte()
        }
        return sound
    }

    /**
     * Gera um som de sino
     */
    private fun generateChime(samples: Int, sampleRate: Int): ByteArray {
        val sound = ByteArray(samples * 2)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = Math.exp(-t * 3) // Decaimento exponencial
            val sample = (Math.sin(2 * Math.PI * 800 * t) + 
                         Math.sin(2 * Math.PI * 1000 * t) * 0.5 + 
                         Math.sin(2 * Math.PI * 1200 * t) * 0.3) * envelope * 0.2
            val sampleShort = (sample * Short.MAX_VALUE).toInt()
            sound[i * 2] = (sampleShort and 0xFF).toByte()
            sound[i * 2 + 1] = ((sampleShort shr 8) and 0xFF).toByte()
        }
        return sound
    }

    /**
     * Gera um som de sino clássico
     */
    private fun generateBell(samples: Int, sampleRate: Int): ByteArray {
        val sound = ByteArray(samples * 2)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = Math.exp(-t * 2) * (1 - t * 0.5) // Envelope mais complexo
            val sample = (Math.sin(2 * Math.PI * 600 * t) + 
                         Math.sin(2 * Math.PI * 800 * t) * 0.7 + 
                         Math.sin(2 * Math.PI * 1000 * t) * 0.5 + 
                         Math.sin(2 * Math.PI * 1200 * t) * 0.3) * envelope * 0.15
            val sampleShort = (sample * Short.MAX_VALUE).toInt()
            sound[i * 2] = (sampleShort and 0xFF).toByte()
            sound[i * 2 + 1] = ((sampleShort shr 8) and 0xFF).toByte()
        }
        return sound
    }

    /**
     * Gera um som de alerta
     */
    private fun generateAlert(samples: Int, sampleRate: Int): ByteArray {
        val sound = ByteArray(samples * 2)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = if (t < 0.1) t * 10 else Math.exp(-(t - 0.1) * 8) // Envelope de ataque rápido
            val sample = Math.sin(2 * Math.PI * 1200 * t) * envelope * 0.3
            val sampleShort = (sample * Short.MAX_VALUE).toInt()
            sound[i * 2] = (sampleShort and 0xFF).toByte()
            sound[i * 2 + 1] = ((sampleShort shr 8) and 0xFF).toByte()
        }
        return sound
    }

    /**
     * Gera um som de notificação suave
     */
    private fun generateNotification(samples: Int, sampleRate: Int): ByteArray {
        val sound = ByteArray(samples * 2)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = Math.exp(-t * 4) * (1 - t * 0.3)
            val sample = (Math.sin(2 * Math.PI * 600 * t) + 
                         Math.sin(2 * Math.PI * 800 * t) * 0.5) * envelope * 0.2
            val sampleShort = (sample * Short.MAX_VALUE).toInt()
            sound[i * 2] = (sampleShort and 0xFF).toByte()
            sound[i * 2 + 1] = ((sampleShort shr 8) and 0xFF).toByte()
        }
        return sound
    }

    /**
     * Reproduz dados de áudio usando AudioTrack
     */
    private fun playAudioData(audioData: ByteArray, sampleRate: Int) {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
            
            audioTrack?.let { track ->
                track.play()
                track.write(audioData, 0, audioData.size)
                
                // Aguardar a reprodução terminar
                Thread {
                    try {
                        while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            Thread.sleep(50)
                        }
                        // O track já parou de tocar, apenas fazer release
                        track.release()
                        audioTrack = null
                    } catch (e: Exception) {
                        // Se houver erro na thread, apenas limpar
                        try {
                            track.release()
                        } catch (releaseException: Exception) {
                            // Ignorar erro de release
                        }
                        audioTrack = null
                    }
                }.start()
            }
        } catch (e: Exception) {
            // Se falhar, usar o som padrão
            playDefaultNotificationSound()
        }
    }

    /**
     * Limpa recursos quando o serviço não é mais necessário
     */
    fun cleanup() {
        stopSoundPreview()
    }
}
