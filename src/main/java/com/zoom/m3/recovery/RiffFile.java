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
        out.writeBytes("WAVE");                             // 9-12 Format always WAVE

        // bext chunk
        writeBextChunk(out);

        // fmt chunk
        out.writeBytes("fmt ");                          // 13-16 chunkID is "fmt " with trailing whitespace
        out.writeInt(Integer.reverseBytes(16));          // 17-20 size of this chunk, is 16 byts
        out.writeShort(Short.reverseBytes((short) 3));      // 21-22 (2 bytes) audioFormat (1 PCM integer, 3 IEEE 754 float)
        out.writeShort(Short.reverseBytes(channels));       // 23-24 (2 bytes) numChannels (1 mono, 2 stereo, 4, etc)
        out.writeInt(Integer.reverseBytes(sampleRate));     // 25-28 (4 bytes) sampleRate (8000, 44100, 48000, etc)
        out.writeInt(Integer.reverseBytes(byteRate));       // 29-32 (4 bytes) byteRate (sampleRate * numChannels * bitsPerSample/8)
        out.writeShort(Short.reverseBytes(blockAlign));     // 33-34 (2 bytes) blockAlign (numChannels * bitsPerSample/8)
        out.writeShort(Short.reverseBytes(bitsPerSample));  // 35-36 (2 bytes) bitsPerSample (8bits, 16bits, 32bits, etc)

        // data chunk
        out.writeBytes("data");                             // 37-40 chunkID ID is "data"
        out.writeInt(Integer.reverseBytes(audioData.length));  // 41-44 size of this chunk varies
        out.write(audioData);
        out.close();

        // write the full size of the file on the 4-8 bytes
        byte[] outArr = byteArrayOutputStream.toByteArray();
        int size = outArr.length - 8;
        ByteBuffer.wrap(outArr, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(size);
        return outArr;
    }

    private static void writeBextChunk(DataOutputStream out) throws IOException {
        // bext chunk
        out.writeBytes("bext");
        out.writeInt(Integer.reverseBytes(256 + 32 + 32 + 10 + 8 + 8 + 8 + 2 + 180 + 4 + 4 + 4 + 4 + 4 + 180)); // bext chunk size (fixed size for BWF)

        // description 256 bytes
        writeToArray(out, 256, "");                     // 256 bytes description
        writeToArray(out, 32, "ZOOM M3");               // 32 bytes originator
        writeToArray(out, 32, "");                      // 32 bytes originator reference
        writeToArray(out, 10, "2023-10-01");            // 10 bytes origination date
        writeToArray(out, 8, "12:00:00");               // 8 bytes origination time
        writeToArray(out, 8, "12:00:00");               // 8 bytes time reference

        out.writeLong(Long.reverseBytes(0L));           // 8 bytes time reference
        out.writeShort(Short.reverseBytes((short) 0));    // 2 bytes version
        out.write(new byte[180]);                         // 180 bytes UMID
        out.writeFloat(0.0f);                          // 4 bytes loudness value
        out.writeFloat(0.0f);                          // 4 bytes loudness range
        out.writeFloat(0.0f);                          // 4 bytes max true peak level
        out.writeFloat(0.0f);                          // 4 bytes max momentary loudness
        out.writeFloat(0.0f);                          // 4 bytes max short term loudness

//        out.write(new byte[180]);                         // 180 bytes reserved for extension
        writeToArray(out, 180, "A=PCM,F=48000,W=32,M=stereo,T=M3;VERSION=1.00;MSRAW=ON ;");


    }

    private static void writeToArray(DataOutputStream out, int arrSize, String str) throws IOException {
        byte[] originator = new byte[arrSize];
        System.arraycopy(str.getBytes(), 0, originator, 0, str.length());
        out.write(originator);
    }
}
