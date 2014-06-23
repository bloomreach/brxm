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

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MockBinaryTest {

    private byte[] data;
    private MockBinary binary;

    @Before
    public void setUp() throws IOException {
        data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte)i;
        }
        binary = new MockBinary(new ByteArrayInputStream(data));
    }

    @Test
    public void getSize() {
        assertEquals(data.length, binary.getSize());
    }

    @Test
    public void getStream() throws IOException {
        IOUtils.contentEquals(new ByteArrayInputStream(data), binary.getStream());
    }

    @Test
    public void readIntoSmallerBuffer() {
        byte[] small = new byte[2];
        int bytesRead = binary.read(small, 0);
        assertEquals(2, bytesRead);
        assertEquals(0, small[0]);
        assertEquals(1, small[1]);
    }

    @Test
    public void readIntoLargerBuffer() {
        byte[] large = new byte[20];
        int bytesRead = binary.read(large, 0);
        assertEquals(10, bytesRead);
        assertEquals(0, large[0]);
        assertEquals(9, large[9]);
        assertEquals(0, large[10]);
    }

    @Test
    public void readFromPosition() {
        byte[] large = new byte[20];
        int bytesRead = binary.read(large, 3);
        assertEquals(7, bytesRead);
        assertEquals(3, large[0]);
        assertEquals(4, large[1]);
        assertEquals(9, large[6]);
        assertEquals(0, large[7]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disposedBinaryThrowsIllegalArgumentExceptionForGetSize() {
        binary.dispose();
        binary.getSize();
    }

}