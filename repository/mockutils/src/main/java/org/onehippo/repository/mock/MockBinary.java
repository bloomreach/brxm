/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.jcr.Binary;

import org.apache.commons.io.IOUtils;

/**
 * Mock version of a JCR binary. All data resides in memory.
 */
public class MockBinary implements Binary {

    private byte[] data;
    private Integer hashCode;

    public MockBinary(final InputStream stream) throws IOException {
        data = IOUtils.toByteArray(stream);
    }

    @Override
    public InputStream getStream() {
        checkNotDisposed();
        return new ByteArrayInputStream(data);
    }

    @Override
    public int read(final byte[] b, final long position) {
        checkNotDisposed();
        int pos = (int)position;
        int amount = Math.min(data.length - pos, b.length);
        System.arraycopy(data, pos, b, 0, amount);
        return amount;
    }

    @Override
    public long getSize() {
        checkNotDisposed();
        return data.length;
    }

    @Override
    public void dispose() {
        data = null;
        hashCode = null;
    }

    private void checkNotDisposed() {
        if (data == null) {
            throw new IllegalArgumentException("Binary has been disposed");
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof MockBinary) {
            MockBinary other = (MockBinary)o;
            return Arrays.equals(data, other.data);
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == null) {
            hashCode = Integer.valueOf(Arrays.hashCode(data));
        }
        return hashCode;
    }

}
