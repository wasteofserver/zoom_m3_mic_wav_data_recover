package com.zoom.m3.recovery;

import picocli.CommandLine;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry class. You should pass as a parameter an img of the card
 * ie: c:\my_card.img
 */
public class ReadFromIso implements Runnable {

    @CommandLine.Option(names = {"-f", "--file"}, description = "The path to the ISO file", required = true)
    private String isoFilename;

    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new ReadFromIso()).execute(args);
        System.exit(exitCode);
    }

    static void openFileNameAndSearchForDataStrings(String filename) throws IOException {
        int current_created_file = 0;
        byte[] dataSequence = new byte[]{0x64, 0x61, 0x74, 0x61}; // reads "data"
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename))) {
            long totalBytesRead = 0;
            byte[] buffer = new byte[Constants.BUFFER_SIZE];
            while (bis.read(buffer) != -1) {
                for (int i = 0; i < buffer.length; i++) {
                    // check if there is a data block
                    if (buffer[i] == dataSequence[0] && buffer[i + 1] == dataSequence[1] && buffer[i + 2] == dataSequence[2] && buffer[i + 3] == dataSequence[3]) {

                        // next 4 bytes represent the size of the data block, as wav and raw and intertwined real value will be double
                        long size = (buffer[i + 4] & 0xFF) |
                                ((buffer[i + 5] & 0xFF) << 8) |
                                ((buffer[i + 6] & 0xFF) << 16) |
                                ((buffer[i + 7] & 0xFF) << 24);

                        long actualSize = size * 2;
                        long startBytes = totalBytesRead + i + 8; // remove "data" and size from the count
                        long endBytes = startBytes + actualSize;

                        // check if the data block is valid
                        if (!Validate.isDataBlockValid(startBytes, startBytes + size, filename)) {
                            System.out.printf("Data sequence at position %d reported size: %d block data: %02X %02X %02X %02X is %s %n",
                                    startBytes, size, buffer[i + 4], buffer[i + 5], buffer[i + 6], buffer[i + 7],
                                    Validate.isDataBlockValid(startBytes, endBytes, filename));
                            continue;
                        }

                        System.out.printf("Data sequence found! Start: %d, End: %d, Size: %d %n", startBytes, endBytes, size);

                        // data block can be too large to fit into memory, so we'll write a tmp file to disk
                        String intertwinedFilenameWithoutHeader = String.format("%03d_tmp_intertwined.wav", current_created_file);
                        SplitZoomM3.saveTmpDataChunkToDisk(intertwinedFilenameWithoutHeader, filename, startBytes, actualSize);

                        for (ZoomM3FileTypes zoomM3FileTypes : ZoomM3FileTypes.values()) {
                            String chunkedFilenameWithoutHeader = String.format("%03d_tmp_intertwined_%s.wav", current_created_file, zoomM3FileTypes);
                            String finalFilename = String.format("%03d_%s.wav", current_created_file, zoomM3FileTypes);
                            Path chunkedFilenameWithoutHeaderFile = Paths.get(chunkedFilenameWithoutHeader);

                            // split the intertwined file into wav and raw files
                            SplitZoomM3.splitChunksToDisk(chunkedFilenameWithoutHeader, intertwinedFilenameWithoutHeader, zoomM3FileTypes);

                            // gets header
                            byte[] header = RiffFile.createRiffHeader(48000, (short) 32, (short) 2, (int) Files.size(chunkedFilenameWithoutHeaderFile));

                            // create final file and delete tmp file
                            SplitZoomM3.appendHeaderToDataChunk(header, chunkedFilenameWithoutHeader, finalFilename);
                            Files.delete(chunkedFilenameWithoutHeaderFile);
                        }
                        // delete tmp file
                        Files.delete(Paths.get(intertwinedFilenameWithoutHeader));
                        current_created_file++;
                    }
                }

                totalBytesRead += buffer.length;
                if (totalBytesRead % (Constants.BUFFER_SIZE * 10000L) == 0) {
                    System.out.println("Read " + totalBytesRead / 1024 / 1024 / 1024 + " gigabytes");
                }
            }
        }
    }

    @Override
    public void run() {
        if (Files.exists(Paths.get(isoFilename))) {
            try {
                openFileNameAndSearchForDataStrings(isoFilename);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.printf("Tried to read from %s which does not exist%n", isoFilename);
        }
    }
}
