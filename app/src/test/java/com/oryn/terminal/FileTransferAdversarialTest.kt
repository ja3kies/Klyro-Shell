package com.oryn.terminal

import org.junit.Test
import org.junit.Assert.*
import java.io.*
import java.nio.file.Files
import java.util.concurrent.*

class FileTransferAdversarialTest {
    private fun refused(action: () -> Unit) { try { action(); fail("Unsafe operation accepted") } catch (expected: IOException) {} }
    @Test fun cancelledBeforeReadWritesNothing() { val out=ByteArrayOutputStream();try { KlyroFileSafety.copyLimited(ByteArrayInputStream(byteArrayOf(1)),out){throw CancellationException()} ;fail() }catch(expected:CancellationException){};assertEquals(0,out.size()) }
    @Test fun cancelledAfterReadCannotWriteChunk() { var checks=0;val out=ByteArrayOutputStream();try { KlyroFileSafety.copyLimited(ByteArrayInputStream(byteArrayOf(1)),out){if(++checks==2)throw CancellationException()};fail() }catch(expected:CancellationException){};assertEquals(0,out.size()) }
    @Test fun stalledProviderCannotSpinForever() { val input=object:InputStream(){override fun read()=0;override fun read(b:ByteArray)=0};refused{KlyroFileSafety.copyLimited(input,ByteArrayOutputStream())} }
    @Test fun interruptedWorkerRetainsInterruptStatus() { Thread.currentThread().interrupt();try{refused{KlyroFileSafety.copyLimited(ByteArrayInputStream(byteArrayOf(1)),ByteArrayOutputStream())};assertTrue(Thread.currentThread().isInterrupted)}finally{Thread.interrupted()} }
    @Test fun emptyDocumentCopiesZeroBytes() { assertEquals(0L,KlyroFileSafety.copyLimited(ByteArrayInputStream(byteArrayOf()),ByteArrayOutputStream())) }
    @Test fun shortReadsPreserveBinaryData() { val input=object:ByteArrayInputStream(ByteArray(100){it.toByte()}){override fun read(b:ByteArray,off:Int,len:Int)=super.read(b,off,minOf(1,len))};val out=ByteArrayOutputStream();assertEquals(100L,KlyroFileSafety.copyLimited(input,out));assertArrayEquals(ByteArray(100){it.toByte()},out.toByteArray()) }
    @Test fun revokedReadPropagatesFailure() { refused{KlyroFileSafety.copyLimited(object:InputStream(){override fun read():Int=throw IOException("revoked")},ByteArrayOutputStream())} }
    @Test fun diskFullPropagatesFailure() { refused{KlyroFileSafety.copyLimited(ByteArrayInputStream(byteArrayOf(1)),object:OutputStream(){override fun write(b:Int){throw IOException("ENOSPC")}})} }
    @Test fun simultaneousImportsNeverOverwriteEachOther() { val dir=Files.createTempDirectory("imports-").toFile();val pool=Executors.newFixedThreadPool(8);try{val futures=(1..8).map{n->pool.submit<File>{val temp=File.createTempFile("stage-",".tmp",dir);temp.writeText("payload-$n");KlyroFileSafety.commitImport(temp,dir,"same.txt")}};val files=futures.map{it.get(3,TimeUnit.SECONDS)};assertEquals(8,files.map{it.name}.toSet().size);assertEquals((1..8).map{"payload-$it"}.toSet(),files.map{it.readText()}.toSet())}finally{pool.shutdownNow();dir.deleteRecursively()} }
    @Test fun preexistingUserFileSurvivesImport() { val dir=Files.createTempDirectory("imports-").toFile();try{File(dir,"same").writeText("user");val temp=File.createTempFile("stage-",".tmp",dir);temp.writeText("new");KlyroFileSafety.commitImport(temp,dir,"same");assertEquals("user",File(dir,"same").readText())}finally{dir.deleteRecursively()} }
    @Test fun foreignTemporaryFileCannotBeMoved() { val dir=Files.createTempDirectory("imports-").toFile();val temp=File.createTempFile("foreign-",".tmp");try{refused{KlyroFileSafety.commitImport(temp,dir,"x")};assertTrue(temp.exists())}finally{temp.delete();dir.deleteRecursively()} }
    @Test fun symlinkTemporaryCannotBeCommitted() { val dir=Files.createTempDirectory("imports-").toFile();try{val file=File(dir,"source").apply{writeText("user")};val link=File(dir,"link");Files.createSymbolicLink(link.toPath(),file.toPath());refused{KlyroFileSafety.commitImport(link,dir,"new")};assertEquals("user",file.readText())}finally{dir.deleteRecursively()} }
    @Test fun missingTemporaryDoesNotCreateTarget() { val dir=Files.createTempDirectory("imports-").toFile();try{refused{KlyroFileSafety.commitImport(File(dir,"missing"),dir,"new")};assertFalse(File(dir,"new").exists())}finally{dir.deleteRecursively()} }
    @Test fun unicodeAndHiddenNamesRoundTripThroughCommit() { val dir=Files.createTempDirectory("imports-").toFile();try{val temp=File.createTempFile("stage-",".tmp",dir);temp.writeText("é😀");val target=KlyroFileSafety.commitImport(temp,dir,".café 😀");assertEquals(".café 😀",target.name);KlyroFileSafety.openExport(dir,target.name).use{assertEquals("é😀",String(it.readBytes(),Charsets.UTF_8))}}finally{dir.deleteRecursively()} }
    @Test fun inaccessibleExportNeverCreatesAnOutput() { val dir=Files.createTempDirectory("exports-").toFile();try{refused{KlyroFileSafety.openExport(dir,"missing")};assertEquals(0,dir.list()!!.size)}finally{dir.deleteRecursively()} }
}
