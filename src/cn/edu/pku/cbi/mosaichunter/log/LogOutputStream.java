/*
 * The MIT License
 *
 * Copyright (c) 2016 Center for Bioinformatics, Peking University
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
