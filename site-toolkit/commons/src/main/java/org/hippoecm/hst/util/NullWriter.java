/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import java.io.IOException;
import java.io.Writer;

public class NullWriter extends Writer {

    public static final NullWriter NULL_WRITER = new NullWriter();

    private NullWriter() {}

    @Override
    public void write(final int c) throws IOException {
        //to /dev/null
    }

    @Override
    public void write(final char[] cbuf) throws IOException {
        //to /dev/null
    }

    @Override
    public void write(final String str) throws IOException {
        //to /dev/null
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        //to /dev/null
    }

    @Override
    public Writer append(final CharSequence csq) throws IOException {
        return this;
    }

    @Override
    public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
        return this;
    }

    @Override
    public Writer append(final char c) throws IOException {
        return this;
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        //to /dev/null
    }

    @Override
    public void flush() throws IOException {
        //to /dev/null
    }

    @Override
    public void close() throws IOException {

    }
}
