package com.zoom.m3.recovery;

import static org.junit.jupiter.api.Assertions.*;

class RiffHeaderTest {

    @org.junit.jupiter.api.Test
    void testRiffHeader() {
        RiffHeader riffHeader = new RiffHeader();
        assertEquals("RIFF", riffHeader.chunkId);
    }



}
