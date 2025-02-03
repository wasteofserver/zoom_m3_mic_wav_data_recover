package com.zoom.m3.recovery;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class Validate {

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
            byte[] buffer = new byte[Constants.BUFFER_SIZE];
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
}
