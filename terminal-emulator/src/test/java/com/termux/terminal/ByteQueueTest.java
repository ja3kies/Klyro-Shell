package com.termux.terminal;

import org.junit.Test;
import java.util.Arrays;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class ByteQueueTest {
    @Test public void availableTracksWrappingReadsAndClosedTail() {
        ByteQueue queue = new ByteQueue(8);
        assertEquals(0, queue.available());
        assertTrue(queue.offer(new byte[]{1, 2, 3, 4, 5, 6}, 0, 6));
        assertEquals(6, queue.available());
        byte[] prefix = new byte[4];
        assertEquals(4, queue.read(prefix, false));
        assertArrayEquals(new byte[]{1, 2, 3, 4}, prefix);
        assertEquals(2, queue.available());
        assertTrue(queue.offer(new byte[]{7, 8, 9, 10, 11}, 0, 5));
        assertEquals(7, queue.available());
        queue.close();
        assertEquals(7, queue.available());
        byte[] tail = new byte[7];
        assertEquals(7, queue.read(tail, false));
        assertArrayEquals(new byte[]{5, 6, 7, 8, 9, 10, 11}, tail);
        assertEquals(0, queue.available());
        assertEquals(-1, queue.read(tail, true));
    }

    @Test(timeout = 1000) public void fullInputQueueRejectsWholePasteWithoutBlocking() {
        ByteQueue queue = new ByteQueue(128 * 1024);
        byte[] paste = new byte[65536 + 12];
        Arrays.fill(paste, (byte) 'x');
        assertTrue(queue.offer(paste, 0, paste.length));
        assertFalse(queue.offer(paste, 0, paste.length));
        byte[] received = new byte[paste.length];
        assertEquals(paste.length, queue.read(received, false));
        assertArrayEquals(paste, received);
        assertEquals(0, queue.read(received, false));
    }

    @Test public void offerPreservesRingOrderAndRangeValidation() {
        ByteQueue queue = new ByteQueue(4);
        assertTrue(queue.offer(new byte[]{1, 2, 3}, 0, 3));
        assertEquals(2, queue.read(new byte[2], false));
        assertTrue(queue.offer(new byte[]{4, 5, 6}, 0, 3));
        byte[] result = new byte[4];
        assertEquals(4, queue.read(result, false));
        assertArrayEquals(new byte[]{3, 4, 5, 6}, result);
        for (int[] range : new int[][]{{-1, 1}, {0, -1}, {1, Integer.MAX_VALUE}}) {
            try {
                queue.offer(result, range[0], range[1]);
                fail("Invalid range accepted");
            } catch (IllegalArgumentException expected) { }
        }
    }

    @Test(timeout = 2000) public void closeWakesBlockedWriterAndDrainsFinalOutput() throws Exception {
        ByteQueue queue = new ByteQueue(4);
        queue.write(new byte[]{1, 2, 3, 4}, 0, 4);
        ExecutorService workers = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> writer = workers.submit(() -> queue.write(new byte[]{5}, 0, 1));
            queue.close();
            assertFalse(writer.get(1, TimeUnit.SECONDS));
            assertFalse(queue.offer(new byte[]{6}, 0, 1));
            byte[] tail = new byte[4];
            assertEquals(4, queue.read(tail, false));
            assertArrayEquals(new byte[]{1, 2, 3, 4}, tail);
            assertEquals(-1, queue.read(tail, true));
        } finally {
            workers.shutdownNow();
        }
    }
}
