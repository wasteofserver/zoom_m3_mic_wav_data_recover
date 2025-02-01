package com.zoom.m3.recovery;

public class SplitZoomM3 {
    final static int CHUNK_SIZE = 262144;

    /**
     * Chunk data splitter
     * <p>
     * Zoom M3 saves audio data in chunks of 262144 bytes, but in intertwines the wav and the raw data.
     * RAW|WAV|RAW|WAV|RAW|WAV
     * This method allows you to easily extract the correct stream
     *
     * @param data       the data to split
     * @param startPoint the start point of the data, RAW or WAV
     * @return the WAV or RAW data
     */
    public static byte[] getBytesChunked(byte[] data, StartAt startPoint) {
        int startAt = (startPoint.equals(StartAt.RAW)) ? 0 : CHUNK_SIZE;

        int totalChunks = data.length / 2;
        byte[] result = new byte[totalChunks + 1000000];
        int resultIndex = 0;

        for (int i = startAt; i < data.length - CHUNK_SIZE; i += 2 * CHUNK_SIZE) {
            if (i + CHUNK_SIZE <= data.length) {
                System.arraycopy(data, i, result, resultIndex, CHUNK_SIZE);
                resultIndex += CHUNK_SIZE;
            }
        }
        return result;
    }

}
