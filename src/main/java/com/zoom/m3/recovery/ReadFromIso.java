package com.zoom.m3.recovery;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReadFromIso {

    final static private int BUFFER_SIZE = 8192 * 16;

    public static void main(String[] args) throws IOException {

        // todo we can't store byte arrays larger than 2GB in memory so we must write them to disk
        // todo we could do it directly in the split zoom

        // look for data blocks in the ISO file
        // when data block is found extract, check size, double it and see if it's valid, consider not valid if it has 16 bytes of 0

        System.out.println("ReadFromIso main");
        String isoFilename = "f:\\68_pierre_zago.img";
        if (Files.exists(Paths.get(isoFilename))) {
            System.out.println("File exists");
            openFileNameAndSearchForDataStrings(isoFilename);
        } else {
            System.out.println("File does not exist");
        }
    }

    static void openFileNameAndSearchForDataStrings(String filename) throws IOException {
        int current_created_file = 0;
        byte[] dataSequence = new byte[]{0x64, 0x61, 0x74, 0x61}; // reads "data"
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename))) {
            long totalBytesRead = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            while (bis.read(buffer) != -1) {
                for (int i = 0; i < buffer.length; i++) {
                    if (buffer[i] == dataSequence[0] && buffer[i + 1] == dataSequence[1] && buffer[i + 2] == dataSequence[2] && buffer[i + 3] == dataSequence[3]) {

                        // next 4 bytes represent the size of the data block, as wav and raw and intertwined we need to double this
                        long size = (buffer[i + 4] & 0xFF) |
                                ((buffer[i + 5] & 0xFF) << 8) |
                                ((buffer[i + 6] & 0xFF) << 16) |
                                ((buffer[i + 7] & 0xFF) << 24);

                        size = size * 2;
                        long startBytes = totalBytesRead + i + 8; // remove "data" and size from the count
                        long endBytes = startBytes + size;

                        if (!isDataBlockValid(startBytes, endBytes-size/2, filename)) {
                            System.out.printf("Data sequence at position %d reported size: %d block data: %02X %02X %02X %02X is %s %n",
                                    startBytes, size, buffer[i + 4], buffer[i + 5], buffer[i + 6], buffer[i + 7],
                                    isDataBlockValid(startBytes, endBytes, filename));
                            continue;
                        }

                        // capture the data block and then save it to disk
                        System.out.printf("Data sequence found! Start: %d, End: %d, Size: %d %n", startBytes, endBytes, size);
                        byte[] dataBlock = readBytesFromRange(filename, startBytes, endBytes);
                        for (StartAt startAt : StartAt.values()) {
                            byte[] cleanStream = SplitZoomM3.getBytesChunked(dataBlock, startAt);
                            byte[] output = RiffFile.createRiffFile(48000, (short) 32, (short) 2, cleanStream);
                            Files.write(Paths.get(String.format("from_iso_%03d_%s.wav", current_created_file, startAt)), output);
                        }
                        current_created_file++;
                    }
                }

                // first data offset should be 34340856
                // first found data offset is   4292607
                totalBytesRead += buffer.length;
                if (totalBytesRead % (BUFFER_SIZE * 10000L) == 0) {
                    System.out.println("Read " + totalBytesRead / 1024 / 1024 / 1024 + " gigabytes");
                }
            }
        }
    }

    /**
     * Check if the data block is a valid sound data block
     * <p>
     * We do a very simple check to see if it has more than one second of data.
     * We then do a slightly longer check but equally simple, we check if there are at least 8 sequencial bytes of data
     * marked as 0x00, which would be very unlikely to appear in a valid sound block
     *
     * @param start    the start of the data block
     * @param end      the end of the data block
     * @param filename the filename of the ISO file
     * @return true if the data block is valid, false otherwise
     * @throws IOException if there is an error reading the file
     */
    static boolean isDataBlockValid(long start, long end, String filename) throws IOException {
        if (end - start < 48000 * 2 * 32 / 8) { // file must have at least one second of data to be considered valid
            return false;
        }
        byte[] dataSequence = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}; // eight straight 0s and we consider invalid
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename))) {
            bis.skip(start);
            long totalBytesRead = start;
            byte[] buffer = new byte[BUFFER_SIZE];
            while (totalBytesRead < end) {
                int bytesToRead = (int) Math.min(buffer.length, end - totalBytesRead);
                int bytesRead = bis.read(buffer, 0, bytesToRead);
                if (bytesRead == -1) {
                    break;
                }
                for (int i = 0; i < buffer.length; i++) {
                    if (buffer[i] == dataSequence[0] &&
                            buffer[i + 1] == dataSequence[1] &&
                            buffer[i + 2] == dataSequence[2] &&
                            buffer[i + 3] == dataSequence[3] &&
                            buffer[i + 4] == dataSequence[4] &&
                            buffer[i + 5] == dataSequence[5] &&
                            buffer[i + 6] == dataSequence[6] &&
                            buffer[i + 7] == dataSequence[7]) {
                        return false;
                    }
                }
                totalBytesRead += bytesRead;
            }
        }
        return true;
    }

    /**
     * Read a range of bytes from a file
     * <p>
     * A simple utility method to capture just the portion of the wav file that we are interested in
     *
     * @param filename the filename of the file to read
     * @param start    the start of the range
     * @param end      the end of the range
     * @return a byte array with the data from the range
     * @throws IOException if there is an error reading the file
     */
    static byte[] readBytesFromRange(String filename, long start, long end) throws IOException {
        // todo we must write this file to a tmp disk file, split and then delete
        Path path = Paths.get(filename);
        long length = end - start;
        int bufferSize = 8192; // 8 KB buffer size
        byte[] buffer = new byte[bufferSize];

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            bis.skip(start);
            long bytesReadTotal = 0;
            while (bytesReadTotal < length) {
                int bytesToRead = (int) Math.min(bufferSize, length - bytesReadTotal);
                int bytesRead = bis.read(buffer, 0, bytesToRead);
                if (bytesRead == -1) {
                    break;
                }
                baos.write(buffer, 0, bytesRead);
                bytesReadTotal += bytesRead;
            }
            return baos.toByteArray();
        }
    }


}
