package com.zoom.m3.recovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    final int CHUNK_SIZE = 262144;
    final String filename = "000.wav";
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
        System.out.println("Main method");
        Main main = new Main();
    }

    public Main() {
        System.out.println("Main constructor");
        System.out.printf("Sample rate: %d\n", sampleRate);
        System.out.printf("Chanel count: %d\n", chanelCount);
        System.out.printf("Bit rate: %d\n", bitRate);
        System.out.printf("Block align: %d\n", blockAlign);
        System.out.printf("Bytes per second: %d\n", bytesPerSecond);

        byte[] fileData = readFileFromFileSystem();
        int lastOffset = -1;
        for (int i = 0; i < fileData.length; i++) {
            if (fileData[i + 0] == dataSequence[0] && fileData[i + 1] == dataSequence[1] && fileData[i + 2] == dataSequence[2] && fileData[i + 3] == dataSequence[3]) {
                lastOffset = i;
                System.out.println("Found data sequence at byte: " + i);
            }
        }

        lastOffset += 4 + 4; // skip the data sequence + 4 bytes for the chunk size

        // we'll now capture all the data from last offset to the end of the file
        byte[] dataChunk = new byte[fileData.length - lastOffset];
        System.arraycopy(fileData, lastOffset, dataChunk, 0, fileData.length - lastOffset);

        byte[] fileA = getBytesChunked(dataChunk, CHUNK_SIZE, 0);
        byte[] fileB = getBytesChunked(dataChunk, CHUNK_SIZE, CHUNK_SIZE);

        byte[] header = RiffHeader.getHeader(fileA.length);
        byte[] fileAWithHeader = new byte[header.length + fileA.length];
        System.arraycopy(header, 0, fileAWithHeader, 0, header.length);
        System.arraycopy(fileA, 0, fileAWithHeader, header.length, fileA.length);

        try {
            Files.write(Paths.get("src/main/resources/" + filename + "_reduced_A.wav"), fileAWithHeader);
//            Files.write(Paths.get("src/main/resources/" + filename + "_reduced_B.wav"), fileB);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void compareChunks(byte[] data, int chunkSize) {
        for (int i = 0; i <= data.length - chunkSize; i += chunkSize) {
            for (int j = i + chunkSize; j <= data.length - chunkSize; j += chunkSize) {
                if (compareChunk(data, i, j, chunkSize)) {
                    System.out.println("Match found between chunks starting at " + i + " and " + j);
                }
            }
        }
    }

    public static boolean compareChunk(byte[] data, int start1, int start2, int chunkSize) {
        for (int k = 0; k < chunkSize; k++) {
            if (data[start1 + k] != data[start2 + k]) {
                return false;
            }
        }
        return true;
    }

    byte[] readFileFromFileSystem() {
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
