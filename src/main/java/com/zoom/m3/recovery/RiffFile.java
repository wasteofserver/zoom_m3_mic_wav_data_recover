package com.zoom.m3.recovery;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RiffFile {

    public static byte[] createRiffFile(int sampleRate, short bitsPerSample, short channels, byte[] audioData) throws IOException {
        // calculate the byte rate, block align and file size
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        short blockAlign = (short) (bitsPerSample * channels / 8);

        // stream that will carry the new RIFF file
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteArrayOutputStream);

        // riff header
        out.writeBytes("RIFF");
        out.writeInt(Integer.reverseBytes(0));
        out.writeBytes("WAVE");                          // 9-12 Format always WAVE
        out.writeBytes("fmt ");                          // 13-16 Subchunk1 ID always "fmt " with trailing whitespace
        out.writeInt(Integer.reverseBytes(16));          // 17-20 Subchunk1 Size always 16
        out.writeShort(Short.reverseBytes((short) 3));      // 21-22 Audio-Format 1 for PCM PulseAudio
        out.writeShort(Short.reverseBytes(channels));       // 23-24 numChannels (1 mono, 2 stereo, 4, etc)
        out.writeInt(Integer.reverseBytes(sampleRate));     // 25-28 sampleRate (8000, 44100, 48000, etc)
        out.writeInt(Integer.reverseBytes(byteRate));       // 29-32 byteRate (8bits, 16bits, 32bits, etc)
        out.writeShort(Short.reverseBytes(blockAlign));     // 33-34 Block Align
        out.writeShort(Short.reverseBytes(bitsPerSample));  // 35-36 Bits-Per-Sample

        // append the actual sound and close the stream
        out.writeBytes("data");// 37-40 Subchunk2 ID always data
        out.writeInt(Integer.reverseBytes(audioData.length));// 41-44 Subchunk2 Size
        out.write(audioData);
        out.close();

        // write the full size of the file on the 4-8 bytes
        byte[] outArr = byteArrayOutputStream.toByteArray();
        int size = outArr.length - 8;
        ByteBuffer.wrap(outArr, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(size);
        return outArr;
    }
}
