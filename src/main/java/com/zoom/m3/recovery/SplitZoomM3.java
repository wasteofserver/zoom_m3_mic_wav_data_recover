package com.zoom.m3.recovery;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SplitZoomM3 {

    /**
     * Chunk data splitter
     * <p>
     * Zoom M3 saves audio data in chunks of 262144 bytes, but it intertwines the wav and the raw data
     * A file is composed of: RAW|WAV|RAW|WAV|RAW|WAV
     * This method allows you to split the chunks
     *
     * @param outputFilename the filename to save the data to
     * @param inputFilename  the filename to read the data from
     * @param startPoint     the start point of the data, RAW or WAV
     * @throws IOException if there is an error reading the file
     */
    public static void splitChunksToDisk(String outputFilename, String inputFilename, ZoomM3FileTypes startPoint) throws IOException {
        int startAtBytes = (startPoint.equals(ZoomM3FileTypes.RAW)) ? 0 : Constants.CHUNK_SIZE;
        Path inputPath = Paths.get(inputFilename);
        Path outputPath = Paths.get(outputFilename);
        outputPath.toFile().deleteOnExit(); // ensure tmpFile is deleted on exit
        byte[] buffer = new byte[Constants.CHUNK_SIZE];

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputPath.toFile()));
             FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            bis.skip(startAtBytes);
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                if (bis.skip(Constants.CHUNK_SIZE) != Constants.CHUNK_SIZE) {
                    break;
                }
            }
            fos.flush();
        }
    }


    /**
     * DataChunk can exceed allowed memory, so we save a tmp file to disk
     *
     * @param outputFilename the filename to save the data to
     * @param inputFilename  the filename to read the data from
     * @param startAtBytes   the start byte to read from
     * @param lengthInBytes  the length of the data to read
     */
    public static void saveTmpDataChunkToDisk(String outputFilename, String inputFilename, long startAtBytes, long lengthInBytes) throws IOException {
        Path inputPath = Paths.get(inputFilename);
        Path outputPath = Paths.get(outputFilename);
        outputPath.toFile().deleteOnExit(); // ensure tmpFile is deleted on exit
        byte[] buffer = new byte[Constants.BUFFER_SIZE];

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputPath.toFile()));
             FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            bis.skip(startAtBytes);
            long bytesReadTotal = 0;
            while (bytesReadTotal < lengthInBytes) {
                int bytesToRead = (int) Math.min(Constants.BUFFER_SIZE, lengthInBytes - bytesReadTotal);
                int bytesRead = bis.read(buffer, 0, bytesToRead);
                if (bytesRead == -1) {
                    break;
                }
                fos.write(buffer, 0, bytesRead);
                bytesReadTotal += bytesRead;
            }
            fos.flush();
        }
    }

    public static void appendHeaderToDataChunk(byte[] riffHeader, String dataChunkFilename, String finalFilename) throws IOException {
        Path inputPath = Paths.get(dataChunkFilename);
        Path outputPath = Paths.get(finalFilename);

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputPath.toFile()));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputPath.toFile()))) {

            // write riff file to output file
            bos.write(riffHeader);

            // Write the original file content to the output file
            byte[] buffer = new byte[Constants.BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            bos.flush();
        }
    }

}
