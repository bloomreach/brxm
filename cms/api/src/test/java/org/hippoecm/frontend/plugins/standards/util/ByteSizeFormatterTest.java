/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.util;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.ThreadContext;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * ByteSizeFormatterTest
 */
public class ByteSizeFormatterTest {

    @Before
    public void before() throws Exception {
        Session session = EasyMock.createNiceMock(Session.class);
        EasyMock.expect(session.getLocale()).andReturn(Locale.FRANCE).anyTimes();
        EasyMock.replay(session);
        ThreadContext.setSession(session);
    }

    @Test
    public void testZeroDecimalPlaces() throws Exception {
        ByteSizeFormatter format = new ByteSizeFormatter(0);

        assertEquals("512 bytes", format.format(512L));

        assertEquals("1 KB", format.format(1024L));
        assertEquals("1 KB", format.format(1034L));
        assertEquals("4 KB", format.format(4096L));
        assertEquals("4 KB", format.format(3584L));
        assertEquals("2 KB", format.format(1584L));

        assertEquals("1 MB", format.format(1048576L));
        assertEquals("4 MB", format.format(4194304L));
        assertEquals("4 MB", format.format(3670016L));
        assertEquals("2 MB", format.format(1670016L));

        assertEquals("1 GB", format.format(1073741824L));
        assertEquals("4 GB", format.format(4294967296L));
        assertEquals("4 GB", format.format(3758096384L));
        assertEquals("3 GB", format.format(3558096384L));
    }

    @Test
    public void testOneDecimalPlaces() throws Exception {
        ByteSizeFormatter format = new ByteSizeFormatter(1);
        
        assertEquals("512 bytes", format.format(512L));

        assertEquals("1 KB", format.format(1024L));
        assertEquals("1 KB", format.format(1034L));
        assertEquals("4 KB", format.format(4096L));
        assertEquals("3,5 KB", format.format(3584L));
        assertEquals("1,5 KB", format.format(1584L));

        assertEquals("1 MB", format.format(1048576L));
        assertEquals("4 MB", format.format(4194304L));
        assertEquals("3,5 MB", format.format(3670016L));
        assertEquals("1,6 MB", format.format(1670016L));

        assertEquals("1 GB", format.format(1073741824L));
        assertEquals("4 GB", format.format(4294967296L));
        assertEquals("3,5 GB", format.format(3758096384L));
        assertEquals("3,3 GB", format.format(3558096384L));
    }

    @Test
    public void testTwoDecimalPlaces() throws Exception {
        ByteSizeFormatter format = new ByteSizeFormatter(2);
        
        assertEquals("512 bytes", format.format(512L));

        assertEquals("1 KB", format.format(1024L));
        assertEquals("1,01 KB", format.format(1034L));
        assertEquals("4 KB", format.format(4096L));
        assertEquals("3,5 KB", format.format(3584L));
        assertEquals("1,55 KB", format.format(1584L));

        assertEquals("1 MB", format.format(1048576L));
        assertEquals("4 MB", format.format(4194304L));
        assertEquals("3,5 MB", format.format(3670016L));
        assertEquals("1,59 MB", format.format(1670016L));

        assertEquals("1 GB", format.format(1073741824L));
        assertEquals("4 GB", format.format(4294967296L));
        assertEquals("3,5 GB", format.format(3758096384L));
        assertEquals("3,31 GB", format.format(3558096384L));
    }

    @Test
    public void testSizeFormattingInUSALocale() throws Exception {
        Session session = EasyMock.createNiceMock(Session.class);
        EasyMock.expect(session.getLocale()).andReturn(Locale.US).anyTimes();
        EasyMock.replay(session);
        ThreadContext.setSession(session);

        long size = 1024 + 512 + 32; // 1.5.. (with more fractions, but rounded to 1.5) KB
        ByteSizeFormatter formatter = new ByteSizeFormatter(1);
        assertEquals("1.5 KB", formatter.format(size));
    }

    @Test
    public void testSizeFormattingInFranceLocale() throws Exception {
        Session session = EasyMock.createNiceMock(Session.class);
        EasyMock.expect(session.getLocale()).andReturn(Locale.FRANCE).anyTimes();
        EasyMock.replay(session);
        ThreadContext.setSession(session);

        long size = 1024 + 512 + 32; // 1.5.. (with more fractions, but rounded to 1.5) KB
        ByteSizeFormatter formatter = new ByteSizeFormatter(1);
        assertEquals("1,5 KB", formatter.format(size));
    }

}
