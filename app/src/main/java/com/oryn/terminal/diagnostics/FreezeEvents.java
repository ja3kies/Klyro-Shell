package com.oryn.terminal.diagnostics;

/** Allowlisted numeric breadcrumbs: never pass a title, command, URI or clipboard here. */
public final class FreezeEvents {
    public enum Code { CREATED, STARTED, RESUMED, PAUSED, STOPPED, DESTROYED, HOME, TERMINAL,
        SETTINGS, REPORTS, PTY_CALLBACK, VIEW_REFRESH, BATCH, SESSION, CURSOR, SCROLL,
        RESIZE, IME, WORKSPACE, SERVICE_START, SERVICE_STOP, STRESS_START, STRESS_END }
    private final long[][] ring=new long[128][4];
    private int next, size;
    private final long[] counts=new long[Code.values().length];
    public synchronized void add(Code code,long monotonic,long a,long b) {
        counts[code.ordinal()]++;
        long[] item=ring[next];item[0]=monotonic;item[1]=code.ordinal();item[2]=a;item[3]=b;
        next=(next+1)%ring.length;size=Math.min(size+1,ring.length);
    }
    public synchronized String snapshot() {
        StringBuilder out=new StringBuilder();
        for (Code code:Code.values()) out.append(code).append("_count=").append(counts[code.ordinal()]).append('\n');
        for(int i=0;i<size;i++) { long[] v=ring[(next-size+i+ring.length)%ring.length];
            out.append(v[0]).append(' ').append(Code.values()[(int)v[1]]).append(' ').append(v[2]).append(' ').append(v[3]).append('\n'); }
        return out.toString();
    }
}
