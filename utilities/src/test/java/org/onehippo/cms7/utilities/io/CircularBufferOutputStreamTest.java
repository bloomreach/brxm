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
    public void testWriteNothing() throws Exception {
        assertEquals(0, new CircularBufferOutputStream(10).toByteArray().length);
    }

}
