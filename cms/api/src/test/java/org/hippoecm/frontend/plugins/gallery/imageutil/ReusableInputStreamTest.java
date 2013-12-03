/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Deprecated
public class ReusableInputStreamTest {

    @Test
    public void testCloseStream() throws Exception{
        InputStream is = getClass().getResourceAsStream("/" + "test-RGB.jpg");
        ReusableInputStream ris = new ReusableInputStream(is);

        try {
            ris.close();
            ris.read();
        } catch (IOException e) {
            fail("IOException should not have been thrown");
        }

        ris.canBeClosed();

        try {
            ris.close();
            ris.read();
            fail("IOException should have been thrown");
        } catch (IOException e) {
            //expected
        }
    }

    @Test
    public void testResetStream() throws Exception{
        InputStream is = getClass().getResourceAsStream("/" + "test-RGB.jpg");
        ReusableInputStream ris = new ReusableInputStream(is);

        byte[] bytesOne = new byte[64];
        int read = ris.read(bytesOne);
        assertEquals(64, read);

        ris.reset();
        byte[] bytesTwo = new byte[64];
        read = ris.read(bytesTwo);
        assertEquals(64, read);

        assertArrayEquals(bytesOne, bytesTwo);
    }

}
