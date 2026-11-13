package com.oryn.terminal.diagnostics;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Worker-only, private atomic reports. Only validated own filenames can be touched. */
public final class FreezeReportStore {
    public static final int MAX_BYTES=512*1024, MAX_REPORTS=10;
    private static final String HEADER="KLYRO_FREEZE_V1\n";
    private final Path directory;
    public FreezeReportStore(File directory) throws IOException {
        this.directory=directory.toPath();
        if(Files.isSymbolicLink(this.directory)) throw new IOException("Unsafe report directory");
        Files.createDirectories(this.directory);
    }
    private Path path(String id) {
        if(id==null || !id.matches("[a-zA-Z0-9-]{1,100}")) throw new IllegalArgumentException("Invalid report ID");
        return directory.resolve(id+".report");
    }
    public synchronized void write(String id,String body) throws IOException {
        byte[] bytes=(HEADER+body).getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES) {
            byte[] suffix="\n[TRUNCATED]\n".getBytes(StandardCharsets.UTF_8);
            int end=MAX_BYTES-suffix.length;
            while(end>0 && (bytes[end]&0xc0)==0x80) end--;
            byte[] bounded=new byte[end+suffix.length];System.arraycopy(bytes,0,bounded,0,end);
            System.arraycopy(suffix,0,bounded,end,suffix.length);bytes=bounded;
        }
        Path target=path(id), temp=Files.createTempFile(directory,".pending-",".tmp");
        try {
            try(FileOutputStream output=new FileOutputStream(temp.toFile())) { output.write(bytes);output.getFD().sync(); }
            Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
        List<String> ids=list();
        while(ids.size()>MAX_REPORTS) { Files.delete(path(ids.remove(0))); }
    }
    public synchronized List<String> list() throws IOException {
        List<String> ids=new ArrayList<>();
        try(DirectoryStream<Path> entries=Files.newDirectoryStream(directory,"*.report")) {
            for(Path p:entries) { String name=p.getFileName().toString();
                String id=name.substring(0,name.length()-7);
                if(!id.matches("[a-zA-Z0-9-]{1,100}") || !Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)
                    || Files.size(p)>MAX_BYTES) continue;
                try(InputStream in=Files.newInputStream(p,LinkOption.NOFOLLOW_LINKS)) {
                    if(Arrays.equals(boundedRead(in, HEADER.length()),HEADER.getBytes(StandardCharsets.UTF_8))) ids.add(id);
                }
            }
        }
        Collections.sort(ids);return ids;
    }
    public synchronized String read(String id) throws IOException {
        if(!list().contains(id)) throw new IOException("Report unavailable");
        try(InputStream input=Files.newInputStream(path(id),LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes=boundedRead(input,MAX_BYTES+1);
            if(bytes.length>MAX_BYTES) throw new IOException("Report exceeds limit");
            return new String(bytes,StandardCharsets.UTF_8);
        }
    }
    public String preview(String id) throws IOException {
        String text=read(id);
        if(text.length()<=8192) return text;
        int end=8192;
        if(Character.isHighSurrogate(text.charAt(end-1))) end--;
        return text.substring(0,end)+"\n[PREVIEW TRUNCATED — export ZIP for full report]";
    }
    public synchronized void delete(String id) throws IOException { Files.deleteIfExists(path(id)); }
    public void exportZip(OutputStream output) throws IOException {
        // Snapshot at most 5 MiB under the store lock; never hold it across a slow SAF write.
        Map<String,String> reports=new LinkedHashMap<>();
        synchronized(this) { for(String id:list()) reports.put(id,read(id)); }
        try(ZipOutputStream zip=new ZipOutputStream(output)) {
            for(Map.Entry<String,String> entry:reports.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()+".txt"));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            }
        }
    }
    private static byte[] boundedRead(InputStream in,int limit) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];
        while(out.size()<limit) { int n=in.read(buffer,0,Math.min(buffer.length,limit-out.size()));
            if(n<0) break;if(n==0) throw new IOException("Trace stream made no progress");out.write(buffer,0,n); }
        return out.toByteArray();
    }
    /** Only Java frame lines are imported; arbitrary trace headers/native dumps are excluded. */
    public static String traceFrames(InputStream input) throws IOException {
        if(input==null) return "SYSTEM_TRACE_UNAVAILABLE\n";
        StringBuilder out=new StringBuilder();
        // Bound bytes before decoding, including a provider that never terminates a line.
        byte[] bytes=boundedRead(input, 64*1024);
        for(String line:new String(bytes,StandardCharsets.UTF_8).split("\n")) {
            String v=line.trim();
            if(v.matches("at [a-zA-Z0-9_.$]+\\.[a-zA-Z0-9_$<>]+\\([a-zA-Z0-9_.$: -]+\\)"))
                out.append(v).append('\n');
            if(out.length()>16000) break;
        }
        return "SYSTEM_TRACE_JAVA_FRAMES_ONLY\n"+out+"[RAW_TRACE_NOT_STORED]\n";
    }
}
