/*
 *  Copyright 2012 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.utilities.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Similar to {@link java.io.ByteArrayOutputStream} except that the byte array buffer does not grow. Instead
 * newly written bytes are added to the front of the buffer, overwriting the previously written bytes when more
 * bytes are written than the capacity of the buffer.
 */
public class CircularBufferOutputStream extends OutputStream {

    protected final byte[] buf;
    protected int cursor = 0;
    protected boolean firstCircleDone = false;

    public CircularBufferOutputStream(int bufferSize) {
        buf = new byte[bufferSize];
    }

    @Override
    public void write(final int b) throws IOException {
        buf[cursor++] = (byte) b;
        cursor = cursor % buf.length;
        if (!firstCircleDone && cursor == 0) {
            firstCircleDone = true;
        }
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        for (int i = off; i < off+len; i++) {
            buf[cursor++] = b[i];
            cursor = cursor % buf.length;
            if (!firstCircleDone && cursor == 0) {
                firstCircleDone = true;
            }
        }
    }

    @Override
    public String toString() {
        return new String(toByteArray());
    }

    public byte[] toByteArray() {
        final byte[] result;
        int i = 0;
        if (!firstCircleDone) {
            result = new byte[cursor];
        } else {
            result = new byte[buf.length];
            for (int j = cursor; j < result.length; i++, j++) {
                result[i] = buf[j];
            }
        }
        for (int j = 0; j < cursor; i++, j++) {
            result[i] = buf[j];
        }
        return result;
    }

}