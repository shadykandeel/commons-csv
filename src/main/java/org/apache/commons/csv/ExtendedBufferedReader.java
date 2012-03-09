/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * ExtendedBufferedReader
 *
 * A special reader decorator which supports more
 * sophisticated access to the underlying reader object.
 *
 * In particular the reader supports a look-ahead option,
 * which allows you to see the next char returned by
 * next().
 */
class ExtendedBufferedReader extends BufferedReader {

    /** The end of stream symbol */
    static final int END_OF_STREAM = -1;

    /** Undefined state for the lookahead char */
    static final int UNDEFINED = -2;

    /** The lookahead chars */
    private int lookaheadChar = UNDEFINED;

    /** The last char returned */
    private int lastChar = UNDEFINED;

    /** The line counter */
    private int lineCounter = 0;

    private CharBuffer line = new CharBuffer();

    /**
     * Created extended buffered reader using default buffer-size
     */
    ExtendedBufferedReader(Reader r) {
        super(r);
        /* note uh: do not fetch the first char here,
        *          because this might block the method!
        */
    }

    /**
     * Reads the next char from the input stream.
     *
     * @return the next char or END_OF_STREAM if end of stream has been reached.
     */
    @Override
    public int read() throws IOException {
        // initialize the lookahead
        if (lookaheadChar == UNDEFINED) {
            lookaheadChar = super.read();
        }
        lastChar = lookaheadChar;
        if (super.ready()) {
            lookaheadChar = super.read();
        } else {
            lookaheadChar = UNDEFINED;
        }
        if (lastChar == '\n') {
            lineCounter++;
        }
        return lastChar;
    }

    /**
     * Returns the last read character again.
     *
     * @return the last read char or UNDEFINED
     */
    int readAgain() {
        return lastChar;
    }

    /**
     * Non-blocking reading of len chars into buffer buf starting
     * at bufferposition off.
     * <p/>
     * performs an iterative read on the underlying stream
     * as long as the following conditions hold:
     * - less than len chars have been read
     * - end of stream has not been reached
     * - next read is not blocking
     *
     * @return nof chars actually read or END_OF_STREAM
     */
    @Override
    public int read(char[] buf, int off, int len) throws IOException {
        // do not claim if len == 0
        if (len == 0) {
            return 0;
        }

        // init lookahead, but do not block !!
        if (lookaheadChar == UNDEFINED) {
            if (ready()) {
                lookaheadChar = super.read();
            } else {
                return -1;
            }
        }
        // 'first read of underlying stream'
        if (lookaheadChar == -1) {
            return -1;
        }
        // continue until the lookaheadChar would block
        int cOff = off;
        while (len > 0 && ready()) {
            if (lookaheadChar == -1) {
                // eof stream reached, do not continue
                return cOff - off;
            } else {
                buf[cOff++] = (char) lookaheadChar;
                if (lookaheadChar == '\n') {
                    lineCounter++;
                }
                lastChar = lookaheadChar;
                lookaheadChar = super.read();
                len--;
            }
        }
        return cOff - off;
    }

    /**
     * @return A String containing the contents of the line, not
     *         including any line-termination characters, or null
     *         if the end of the stream has been reached
     */
    @Override
    public String readLine() throws IOException {

        if (lookaheadChar == UNDEFINED) {
            lookaheadChar = super.read();
        }

        line.clear(); //reuse

        // return null if end of stream has been reached
        if (lookaheadChar == END_OF_STREAM) {
            return null;
        }
        // do we have a line termination already
        char laChar = (char) lookaheadChar;
        if (laChar == '\n' || laChar == '\r') {
            lastChar = lookaheadChar;
            lookaheadChar = super.read();
            // ignore '\r\n' as well
            if ((char) lookaheadChar == '\n') {
                lastChar = lookaheadChar;
                lookaheadChar = super.read();
            }
            lineCounter++;
            return line.toString();
        }

        // create the rest-of-line return and update the lookahead
        line.append(laChar);
        String restOfLine = super.readLine(); // TODO involves copying
        lastChar = lookaheadChar;
        lookaheadChar = super.read();
        if (restOfLine != null) {
            line.append(restOfLine);
        }
        lineCounter++;
        return line.toString();
    }

    /**
     * Unsupported
     */
    @Override
    public long skip(long n) throws IllegalArgumentException, IOException {
        throw new UnsupportedOperationException("CSV has no reason to implement this");
    }

    /**
     * Returns the next char in the stream without consuming it.
     *
     * Remember the next char read by read(..) will always be
     * identical to lookAhead().
     *
     * @return the next char (without consuming it) or END_OF_STREAM
     */
    int lookAhead() throws IOException {
        if (lookaheadChar == UNDEFINED) {
            lookaheadChar = super.read();
        }
        return lookaheadChar;
    }


    /**
     * Returns the nof line read
     *
     * @return the current-line-number (or -1)
     */
    int getLineNumber() {
        return lineCounter > -1 ? lineCounter : -1;
    }

    /**
     * Unsupported.
     * @throws UnsupportedOperationException if invoked
     */
    @Override
    public boolean markSupported() {
        throw new UnsupportedOperationException("CSV has no reason to implement this");
    }

}