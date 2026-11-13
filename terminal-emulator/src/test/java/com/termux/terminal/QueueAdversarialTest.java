package com.termux.terminal;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class QueueAdversarialTest {
    @Test(timeout=3000) public void concurrentOffersAreWholeTransactions()throws Exception{ByteQueue q=new ByteQueue(16);ExecutorService pool=Executors.newFixedThreadPool(2);try{byte[] a=new byte[8],b=new byte[8];Arrays.fill(a,(byte)1);Arrays.fill(b,(byte)2);Future<Boolean>x=pool.submit(()->q.offer(a,0,8)),y=pool.submit(()->q.offer(b,0,8));assertTrue(x.get());assertTrue(y.get());byte[] out=new byte[16];assertEquals(16,q.read(out,false));for(int i=1;i<8;i++)assertEquals(out[0],out[i]);for(int i=9;i<16;i++)assertEquals(out[8],out[i]);assertNotEquals(out[0],out[8]);}finally{pool.shutdownNow();}}
    private void awaitWaiting(Thread thread) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (thread.getState() != Thread.State.WAITING && System.nanoTime() < deadline)
            Thread.yield();
        assertEquals("worker must actually block before close", Thread.State.WAITING, thread.getState());
    }
    @Test(timeout=3000) public void closeReleasesBlockedStdoutProducer() throws Exception {
        ByteQueue q = new ByteQueue(4);
        q.offer(new byte[4], 0, 4);
        FutureTask<Boolean> write = new FutureTask<>(() -> q.write(new byte[100], 0, 100));
        Thread worker = new Thread(write);
        worker.start();
        try {
            awaitWaiting(worker);
            q.close();
            assertFalse(write.get(1, TimeUnit.SECONDS));
            assertEquals(4, q.read(new byte[4], false));
        } finally { q.close(); worker.join(1000); }
    }
    @Test(timeout=3000) public void closeReleasesWaitingConsumer() throws Exception {
        ByteQueue q = new ByteQueue(4);
        FutureTask<Integer> read = new FutureTask<>(() -> q.read(new byte[4], true));
        Thread worker = new Thread(read);
        worker.start();
        try {
            awaitWaiting(worker);
            q.close();
            assertEquals(Integer.valueOf(-1), read.get(1, TimeUnit.SECONDS));
        } finally { q.close(); worker.join(1000); }
    }
    @Test(timeout=1000) public void oversizedOfferRejectsWithoutPartialPrefix(){ByteQueue q=new ByteQueue(4);assertFalse(q.offer(new byte[5],0,5));assertEquals(0,q.read(new byte[4],false));}
    @Test public void ringWrapKeepsUnreadInputInOrder(){ByteQueue q=new ByteQueue(4);q.offer(new byte[]{1,2,3,4},0,4);q.read(new byte[3],false);q.offer(new byte[]{5,6,7},0,3);byte[] out=new byte[4];q.read(out,false);assertArrayEquals(new byte[]{4,5,6,7},out);}
    @Test public void rejectionDoesNotDestroyPendingInput(){ByteQueue q=new ByteQueue(4);q.offer(new byte[]{1,2,3},0,3);assertFalse(q.offer(new byte[]{4,5},0,2));byte[] out=new byte[3];q.read(out,false);assertArrayEquals(new byte[]{1,2,3},out);}
    @Test public void closedQueueRejectsAllNewInputButDrainsFinalBytes(){ByteQueue q=new ByteQueue(4);q.offer(new byte[]{1},0,1);q.close();assertFalse(q.offer(new byte[]{2},0,1));assertEquals(1,q.read(new byte[4],false));assertEquals(-1,q.read(new byte[4],false));}
    @Test(timeout=10000) public void fourMegabyteOutputIsLosslessUnderBackpressure()throws Exception{ByteQueue q=new ByteQueue(4096);ExecutorService p=Executors.newSingleThreadExecutor();try{Future<?> producer=p.submit(()->{byte[] b=new byte[32768];for(int i=0;i<b.length;i++)b[i]=(byte)i;for(int i=0;i<128;i++)assertTrue(q.write(b,0,b.length));q.close();});long count=0,start=System.nanoTime();byte[] read=new byte[2003];int n;while((n=q.read(read,true))>=0){for(int i=0;i<n;i++)assertEquals((byte)(count+i),read[i]);count+=n;}producer.get(2,TimeUnit.SECONDS);assertEquals(4194304L,count);System.out.println("queue_bytes="+count+" elapsed_ns="+(System.nanoTime()-start));}finally{p.shutdownNow();}}
    @Test public void invalidRangeCannotModifyQueue(){ByteQueue q=new ByteQueue(4);try{q.offer(new byte[4],2,Integer.MAX_VALUE);fail();}catch(IllegalArgumentException expected){}assertEquals(0,q.read(new byte[4],false));}
}
