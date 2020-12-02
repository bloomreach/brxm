/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.repository.valuelist;

import java.util.Locale;

import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.utils.JcrUtils;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createPartialMock;
import static org.powermock.api.easymock.PowerMock.expectPrivate;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.internal.*", "javax.xml.parsers.*", "org.w3c.dom.*", "org.xml.sax.*", "javax.net.ssl.*"})
@PrepareForTest({ValueListServiceImpl.class, JcrUtils.class})
public class ValueListServiceImplTest {
    
    private ValueListService valueListService = ValueListService.get();

    @Before
    public void setup() {
        PowerMock.mockStatic(JcrUtils.class);
        
        // clear value list service cache
        valueListService.invalidateCache();
    }


    @Test
    public void getValueList() {
        final String source = "/path/to/valuelist";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");
        final ValueList valueList = PowerMock.createMock(ValueList.class);

        expect(JcrUtils.getValueList(source, locale, session)).andReturn(valueList);

        replayAll();

        assertThat(valueListService.getValueList(source, locale, session), equalTo(valueList));

        verifyAll();
    }

    @Test
    public void getValueListWithoutLocale() {
        final String source = "/path/to/valuelist";
        final Session session = createMock(Session.class);
        final Locale locale = null;
        final ValueList valueList = PowerMock.createMock(ValueList.class);

        expect(JcrUtils.getValueList(source, locale, session)).andReturn(valueList);

        replayAll();

        assertThat(valueListService.getValueList(source, locale, session), equalTo(valueList));

        verifyAll();
    }

    @Test
    public void valueListAresCached() throws Exception {
        final String source = "/path/to/list";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");
        final ValueList valueList = createMock(ValueList.class);
        final String method = "retrieveValueList";

        final ValueListService valueListServiceMock = createPartialMock(ValueListServiceImpl.class, method);
        expectPrivate(valueListServiceMock, method, source, locale, session).andReturn(valueList);

        replayAll();

        final ValueList ValueList1 = valueListServiceMock.getValueList(source, locale, session);
        final ValueList ValueList2 = valueListServiceMock.getValueList(source, locale, session);
        assertThat(ValueList1, sameInstance(ValueList2));

        verifyAll();
    }

    @Test
    public void ValueListCacheCanBeInvalidated() throws Exception {
        final String source = "/path/to/list";
        final Session session = createMock(Session.class);
        final Locale locale = new Locale("en");
        final ValueList docType = createMock(ValueList.class);
        final String method = "retrieveValueList";

        final ValueListService valueListServiceMock = createPartialMock(ValueListServiceImpl.class, method);
        expectPrivate(valueListServiceMock, method, source, locale, session).andReturn(docType).times(2);

        replayAll();

        valueListServiceMock.getValueList(source, locale, session);
        valueListServiceMock.invalidateCache();
        valueListServiceMock.getValueList(source, locale, session);

        verifyAll();
    }

}