package com.zoom.m3.recovery;

import java.io.*;

public class EmptyWave {

    public static void main(String[] args) {
//        try {
//            createEmptyWaveFile(48000, (short) 32, (short) 2, 10, new File("empty.wav"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public static void createEmptyWaveFile(int sampleRate, short bitsPerSample, short channels, File file, byte[] audioData) throws IOException {
        System.out.println("data length: " + audioData.length);

        // calculate some
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        short blockAlign = (short) (bitsPerSample * channels / 8);


        int fileSize = audioData.length + 44 - 8;

        // The stream that writes the audio file to the disk
        DataOutputStream out = new DataOutputStream(new FileOutputStream(file));

        // Write Header
        out.writeBytes("RIFF");// 0-4 ChunkId always RIFF
        out.writeInt(Integer.reverseBytes(fileSize));// 5-8 ChunkSize always audio-length +header-length(44)
        out.writeBytes("WAVE");// 9-12 Format always WAVE
        out.writeBytes("fmt ");// 13-16 Subchunk1 ID always "fmt " with trailing whitespace
        out.writeInt(Integer.reverseBytes(16)); // 17-20 Subchunk1 Size always 16
        out.writeShort(Short.reverseBytes((short) 3));// 21-22 Audio-Format 1 for PCM PulseAudio
        out.writeShort(Short.reverseBytes(channels));// 23-24 Num-Channels 1 for mono, 2 for stereo
        out.writeInt(Integer.reverseBytes(sampleRate));// 25-28 Sample-Rate
        out.writeInt(Integer.reverseBytes(byteRate));// 29-32 Byte Rate
        out.writeShort(Short.reverseBytes(blockAlign));// 33-34 Block Align
        out.writeShort(Short.reverseBytes(bitsPerSample));// 35-36 Bits-Per-Sample

        // Append the silent audio data or what you recorded from the mic
        out.writeBytes("data");// 37-40 Subchunk2 ID always data
        out.writeInt(Integer.reverseBytes(audioData.length));// 41-44 Subchunk2 Size
        out.write(audioData);
        out.close();// close the stream properly
    }
}
