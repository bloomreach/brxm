/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class CircularBufferOutputStreamTest {

    @Test
    public void testWriteLessThanBufferSize() throws IOException {
        final String testString = "abcde";
        final CircularBufferOutputStream cbos = new CircularBufferOutputStream(10);
        cbos.write(testString.getBytes());
        assertEquals(testString, cbos.toString());
    }

    @Test
    public void testWriteMoreThanBufferSize() throws Exception {
        final String testString = "abcde";
        final CircularBufferOutputStream cbos = new CircularBufferOutputStream(3);
        cbos.write(testString.getBytes());
        assertEquals("cde", cbos.toString());
    }

    @Test
    public void testWriteBufferSizeCharacters() throws Exception {
        final String testString = "abcde";
        final CircularBufferOutputStream cbos = new CircularBufferOutputStream(5);
        cbos.write(testString.getBytes());
        assertEquals(testString, cbos.toString());
    }

    @Test
    public void testWriteNothing() throws Exception {
        assertEquals(0, new CircularBufferOutputStream(10).toByteArray().length);
    }

    @Test
    public void testWriteZeros() throws Exception {
        final byte[] zeros = new byte[] {0, 0, 1, 0, 0, 0};
        final CircularBufferOutputStream circularBuffer = new CircularBufferOutputStream(5);
        for (int i=0; i<zeros.length; i++) {
            circularBuffer.write(zeros[i]);
        }
        final byte[] expectedResult = new byte[] {0, 1, 0, 0, 0};
        final byte[] result = circularBuffer.toByteArray();

        assertEquals(expectedResult.length, result.length);

        for (int i=0; i<expectedResult.length; i++) {
            assertEquals(expectedResult[i], result[i]);
        }
    }

    @Test
    @Ignore
    public void testWriteMoreThanMaxInteger() throws Exception {
        final CircularBufferOutputStream circularBuffer = new CircularBufferOutputStream(100);
        long moreThanMaxInteger = 2 + (long) Integer.MAX_VALUE;
        for (long i=0; i<moreThanMaxInteger; i++) {
            circularBuffer.write((byte) Math.random());
        }
    }

}
