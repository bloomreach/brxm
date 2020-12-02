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
package org.onehippo.forge.selection.repository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.repository.valuelist.ValueListService;
import org.onehippo.jaxrs.cxf.CXFTest;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.internal.*", "javax.xml.parsers.*", "org.w3c.dom.*", "org.xml.sax.*", "javax.net.ssl.*"})
@PrepareForTest({ValueListService.class})
public class ValueListResourceTest extends CXFTest {

    private Session userSession;
    private ValueListService valueListService;

    @Before
    public void setup() {
        userSession = createMock(Session.class);
        valueListService = createMock(ValueListService.class);

        final SessionRequestContextProvider sessionRequestContextProvider = createMock(SessionRequestContextProvider.class);
        expect(sessionRequestContextProvider.getJcrSession(anyObject())).andReturn(userSession).anyTimes();
        replay(sessionRequestContextProvider);

        PowerMock.mockStaticPartial(ValueListService.class, "get");
        expect(ValueListService.get()).andReturn(valueListService).anyTimes();
        replayAll();

        final CXFTest.Config config = new CXFTest.Config();
        config.addServerSingleton(new ValueListResource(sessionRequestContextProvider));
        config.addServerSingleton(new JacksonJsonProvider());

        setup(config);
    }

    @Test
    public void getValueListWithoutLocale() {
        final String source = "/path/to/valuelist";
        final Locale locale = null;
        final ValueList testValuelist = getTestValueList();

        expect(valueListService.getValueList(eq(source), eq(locale), eq(userSession))).andReturn(testValuelist);
        replay(valueListService);

        final String expectedBody = normalizeJsonResource("/test-document.json");

        when()
                .get(source)
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));

    }

    @Test
    public void getValueListWithEmptyLocale() {
        final String source = "/path/to/valuelist";
        final Locale locale = null;
        final ValueList testValuelist = getTestValueList();

        expect(valueListService.getValueList(eq(source), eq(locale), eq(userSession))).andReturn(testValuelist);
        replay(valueListService);

        final String expectedBody = normalizeJsonResource("/test-document.json");

        when()
                .get(source + "?locale=")
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));

    }

    @Test
    public void getValueListWithLocale() {
        final String source = "/path/to/valuelist";
        final Locale locale = Locale.ENGLISH;
        final ValueList testValuelist = getTestValueList();

        expect(valueListService.getValueList(eq(source), eq(locale), eq(userSession))).andReturn(testValuelist);
        replay(valueListService);

        final String expectedBody = normalizeJsonResource("/test-document.json");

        when()
                .get(source + "?locale=en")
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));

    }

    @Test
    public void getValueListWithInvalidLocale() {
        final String source = "/path/to/valuelist";
        final Locale locale = null;
        final ValueList testValuelist = getTestValueList();

        expect(valueListService.getValueList(eq(source), eq(locale), eq(userSession))).andReturn(testValuelist);
        replay(valueListService);

        final String expectedBody = normalizeJsonResource("/test-document.json");

        when()
                .get(source + "?locale=7_9")
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));

    }

    @Test
    public void getValueListSorted() {
        final String source = "/path/to/valuelist";
        final Locale locale = Locale.ENGLISH;
        final ValueList testValuelist = getTestValueList();

        expect(valueListService.getValueList(eq(source), eq(locale), eq(userSession))).andReturn(testValuelist);
        replay(valueListService);

        final String expectedBody = normalizeJsonResource("/test-document-sorted.json");

        when()
                .get(source + "?locale=en&sortComparator=org.onehipppo.forge.selection.comparator.TestComparator")
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));
    }

    @Test
    public void getValueListWithEmptySortComparator() {
        final String source = "/path/to/valuelist";
        final Locale locale = Locale.ENGLISH;
        final ValueList testValuelist = getTestValueList();

        expect(valueListService.getValueList(eq(source), eq(locale), eq(userSession))).andReturn(testValuelist);
        replay(valueListService);

        final String expectedBody = normalizeJsonResource("/test-document.json");

        when()
                .get(source + "?locale=en&sortComparator=")
                .then()
                .statusCode(200)
                .body(equalTo(expectedBody));
    }

    @Test
    public void getValueListSortedWithEmptySortOptions() {
        final String source = "/path/to/valuelist";
        final Locale locale = Locale.ENGLISH;
        final ValueList testValuelist = getTestValueList();

        expect(valueListService.getValueList(eq(source), eq(locale), eq(userSession))).andReturn(testValuelist);
        replay(valueListService);

        final String expectedBody = normalizeJsonResource("/test-document-sorted.json");

        when()
                .get(source + "?locale=en&sortComparator=org.onehipppo.forge.selection.comparator.TestComparator" +
                        "&sortOrder=&sortBy=")
        .then()
                .statusCode(200)
                .body(equalTo(expectedBody));
    }

    private String normalizeJsonResource(final String resourcePath) {
        final InputStream resourceStream = ValueListResourceTest.class.getResourceAsStream(resourcePath);
        return new BufferedReader(new InputStreamReader(resourceStream))
                .lines()
                .map(String::trim)
                .collect(Collectors.joining(""));
    }

    private ValueList getTestValueList() {
        final ValueList valueList = new ValueList();
        valueList.add(new ListItem("1", "Zorro"));
        valueList.add(new ListItem("2", "Maria"));
        return valueList;
    }
}