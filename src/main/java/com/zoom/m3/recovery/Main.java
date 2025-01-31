package com.zoom.m3.recovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    final int CHUNK_SIZE = 262144;
    //    final String filename = "001.wav";
    int sampleRate = 48000;      // 44.1 kHz
    int chanelCount = 2;             // mic records in stereo
    int bitRate = 32;                   // 32-bit float recording
    int blockAlign = bitRate / 8 * chanelCount;  // block in bytes
    int bytesPerSecond = blockAlign * sampleRate;
    // number of bytes per second is block align * sample rate
    //    Chunk ID	4	0x64 0x61 0x74 0x61 (i.e. "data")
    //    Chunk Body Size	4	32-bit unsigned integer
    byte[] dataSequence = new byte[]{0x64, 0x61, 0x74, 0x61};

    public static void main(String[] args) {
        new Main("000.wav");

//        for (int i = 0; i < 43; i++) {
//            if (i == 10 || i == 11 || i == 12 || i == 13 || i == 27 || i == 34) continue;
//            String s = String.format("%03d.wav", i);
//            System.out.println(s);
//            new Main(s);
//        }
    }

    public Main(String filename) {
        System.out.println("Main constructor");
        System.out.printf("Sample rate: %d\n", sampleRate);
        System.out.printf("Chanel count: %d\n", chanelCount);
        System.out.printf("Bit rate: %d\n", bitRate);
        System.out.printf("Block align: %d\n", blockAlign);
        System.out.printf("Bytes per second: %d\n", bytesPerSecond);

        byte[] fileData = readFileFromFileSystem(filename);
        byte[] dataChunk = getDataChunkOfRiffFile(fileData);

        byte[] fileA = getBytesChunked(dataChunk, CHUNK_SIZE, 0);
        byte[] fileB = getBytesChunked(dataChunk, CHUNK_SIZE, CHUNK_SIZE); // this will be the raw file

        try {
            byte[] output = RiffFile.createRiffFile(48000, (short) 32, (short) 2, fileA);
            Files.write(Paths.get(filename + "_header_auto.wav"), output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Extract the data chunk from the RIFF file
     *
     * @param fileData the RIFF file as a byte array
     * @return the data chunk as a byte array
     */
    private byte[] getDataChunkOfRiffFile(byte[] fileData) {
        int lastOffset = -1;
        for (int i = 0; i < fileData.length; i++) {
            if (fileData[i + 0] == dataSequence[0] && fileData[i + 1] == dataSequence[1] && fileData[i + 2] == dataSequence[2] && fileData[i + 3] == dataSequence[3]) {
                lastOffset = i;
                System.out.println("Found data sequence at byte: " + i);
            }
        }
        lastOffset += 4 + 4; // skip the data sequence + 4 bytes for the chunk size
        byte[] dataChunk = new byte[fileData.length - lastOffset];
        System.arraycopy(fileData, lastOffset, dataChunk, 0, fileData.length - lastOffset);
        return dataChunk;
    }


    byte[] readFileFromFileSystem(String filename) {
        Path path = Paths.get("src/main/resources/" + filename);
        try {
            byte[] data = Files.readAllBytes(path);
            System.out.println("File read successfully, size: " + data.length + " bytes");
            // You can now work with the byte array 'data'
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    byte[] getBytesChunked(byte[] data, int chunkSize, int startAt) {
        int totalChunks = data.length / 2;
        byte[] result = new byte[totalChunks + 1000000];
        int resultIndex = 0;

        for (int i = startAt; i < data.length - chunkSize; i += 2 * chunkSize) {
            if (i + chunkSize <= data.length) {
                System.arraycopy(data, i, result, resultIndex, chunkSize);
                resultIndex += chunkSize;
            }
        }
        return result;
    }


}
