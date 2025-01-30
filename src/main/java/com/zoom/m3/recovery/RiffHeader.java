package com.zoom.m3.recovery;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Header fields were taken from here
 * <a href="http://soundfile.sapp.org/doc/WaveFormat/">...</a>
 */
public class RiffHeader {

    public static byte[] stringToBytesLittleEndian(String str) {
        char[] charArray = str.toCharArray();
        byte[] byteArray = new byte[charArray.length];
        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }
        return byteArray;
    }

    public static byte[] getHeader(int dataSize) {
        System.out.println("Data size: " + dataSize);

        int channels = 2;
        int sampleRate = 48000;
        int bitsPerSample = 32;
        int byteRate = sampleRate * bitsPerSample * channels / 8;
        int blockAlign = channels * bitsPerSample / 8;


        try {
            File file = new File("teste.wav");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteArrayOutputStream);
            out.writeBytes("RIFF");
//            out.write(Integer.reverseBytes(dataSize + 44)); // 5-8 ChunkSize
            out.write(Integer.reverseBytes(7078464)); // 5-8 ChunkSize
//            out.write(new byte[]{(byte) 0xF8, 0x7F, (byte) 0xBD, 0x00}); // This is the size of the  entire file in bytes minus 8 bytes for the two fields not included in this count: ChunkID and ChunkSize.
            out.writeBytes("WAVE");
//            out.writeBytes("bextZ");
//            for (int i = 0; i < 259; i++) {
//                out.writeByte(0);
//            }
//            out.writeBytes("ZOOM M3");
//            for (int i = 0; i < 57; i++) {
//                out.writeByte(0);
//            }
//            out.writeBytes("2022-01-0609:22:54");
//            out.write(new byte[]{0x00, (byte) 0xD1, (byte) 0xA0, 0x60});
//            for (int i = 0; i < 260; i++) {
//                out.writeByte(0);
//            }
//            out.writeBytes("A=PCM,F=48000,W=32,M=stereo,T=M3;VERSION=1.00;MSRAW=ON ;");
//            for (int i = 0; i < 456; i++) {
//                out.writeByte(0);
//            }
            out.writeBytes("fmt ");
            out.writeInt(Integer.reverseBytes(16));             // 17-20 Subchunk1 Size always 16
            out.writeShort(Short.reverseBytes((short) 1));      // 21-22 Audio-Format 1 for PCM PulseAudio
            out.writeShort(Integer.reverseBytes(channels));    // 23-24 Num-Channels 1 for mono, 2 for stereo
            out.writeInt(Integer.reverseBytes(sampleRate)); // 25-28 Sample-Rate
            out.writeInt(Integer.reverseBytes(byteRate));// 29-32 Byte Rate
            out.writeShort(Integer.reverseBytes(blockAlign));// 33-34 Block Align
            out.writeShort(Integer.reverseBytes(bitsPerSample));// 35-36 Bits-Per-Sample
//            out.write(new byte[]{0x66, 0x6D, 0x74, 0x20, 0x10, 0x00, 0x00, 0x00, 0x03, 0x00, 0x02, 0x00, (byte) 0x80, (byte) 0xBB, 0x00, 0x00, 0x00, (byte) 0xDC, 0x05, 0x00, 0x08, 0x00, 0x20, 0x00, 0x50, 0x41, 0x44, 0x20, 0x6A, (byte) 0xFB, 0x01});
//            for (int i = 0; i < 129899; i++) {
//                out.writeByte(0);
//            }
            out.writeBytes("data");
            out.write(Integer.reverseBytes(dataSize));
//            out.write(new byte[]{0x00, (byte) 0x80, (byte) 0xBB, 0x00});

            out.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
