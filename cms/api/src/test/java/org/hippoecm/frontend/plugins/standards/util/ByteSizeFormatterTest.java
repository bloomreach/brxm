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
import org.junit.Test;

/**
 * ByteSizeFormatterTest
 */
public class ByteSizeFormatterTest {

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
