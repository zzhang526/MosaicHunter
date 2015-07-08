package cn.edu.pku.cbi.mosaichunter.log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class LogOutputStream extends OutputStream {

    private final OutputStream log;
    private final PrintStream std;
    
    public LogOutputStream(String logFile, PrintStream std) throws IOException {
        this(logFile == null ? null : new FileOutputStream(logFile), std);
    }
    
    public LogOutputStream(OutputStream logOutputStream, PrintStream std) {
        this.log = logOutputStream;
        this.std = std;
    }
    
    @Override
    public void write(int b) throws IOException {
        if (log != null) {
            log.write(b);
        } 
        if (log != null) {
            std.write(b);
        } 
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (log != null) {
            log.write(b, off, len);
        } 
        if (log != null) {
            std.write(b, off, len);
        } 
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        if (log != null) {
            log.write(b);
        } 
        if (log != null) {
            std.write(b);
        } 
    }
    
    @Override
    public void flush() throws IOException {
        if (log != null) {
            log.flush();
        }
        if (std != null) {
            std.flush();
        }
    }
    
    @Override
    public void close() throws IOException {
        if (log != null) {
            log.close();
        }
        if (std != null) {
            std.close();
        }
    }
    
}
