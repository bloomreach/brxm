/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.restapi.content.requests;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.restapi.AbstractRestApiIT;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DocumentsResourceIT extends AbstractRestApiIT {

    private static final Logger log = LoggerFactory.getLogger(DocumentsResourceIT.class);

    private static ObjectMapper mapper = new ObjectMapper();
    private static String PROPERTY_TITLE = "myhippoproject:title";
    private static String PROPERTY_DATE = "myhippoproject:date";

    @Test
    public void test_about_us_document() throws Exception {

        // about-us  handle identifier is 'ebebebeb-5fa8-48a8-b03b-4524373d992b'
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/api/documents/30092f4e-2ef7-4c72-86a5-8ce895908937", null);

        final MockHttpServletResponse response = render(requestResponse);

        final String restResponse = response.getContentAsString();
        assertTrue(StringUtils.isNotEmpty(restResponse));

        final Map<String, Object> deserializedAboutUs = mapper.reader(Map.class).readValue(restResponse);
        assertEquals("about-us", deserializedAboutUs.get("name"));
        // TODO assertEquals("published", deserializedAboutUs.get("hippostd:state"));
        // TODO assertEquals("2010-01-21T12:34:11.055+02:00", deserializedAboutUs.get("hippostdpubwf:creationDate"));

        // TODO final ImmutableList<String> mixins = ImmutableList.of("hippotranslation:translated", "mix:versionable");
        // TODO assertEquals(mixins, deserializedAboutUs.get("jcr:mixinTypes"));
    }

    @Test
    public void non_handle_nodes_are_not_allowed() throws Exception {
        // ebebebeb-5fa8-48a8-b03b-4524373d992a is folder node
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/api/documents/ebebebeb-5fa8-48a8-b03b-4524373d992a", null);
        final MockHttpServletResponse response = render(requestResponse);
        assertEquals(SC_NOT_FOUND, response.getStatus());
        assertTrue(response.getContentAsString().contains("not found below scope '/api'"));
    }

    @Test
    public void handle_node_of_content_outside_api_mount_is_not_allowed() throws Exception {
        // a62a34ae-5f42-4482-a27a-7f39459ec8ee homepage of subsite
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/api/documents/a62a34ae-5f42-4482-a27a-7f39459ec8ee", null);
        final MockHttpServletResponse response = render(requestResponse);
        assertEquals(SC_NOT_FOUND, response.getStatus());
        assertTrue(response.getContentAsString().contains("not found below scope '/api'"));
    }

    @Test
    public void test_global_content_api() throws Exception {
        final Session session = createSession("admin", "admin");
        try {
            final Node medusaNews = session.getNode("/unittestcontent/documents/myhippoproject/news/2015/12/the-medusa-news");

            final RequestResponseMock requestResponse = mockGetRequestResponse("http", "onehippo.io", "/documents/" + medusaNews.getIdentifier(), null);
            final MockHttpServletResponse response = render(requestResponse);
            final String restResponse = response.getContentAsString();

            final Map<String, Object> deserializedAboutUs = mapper.reader(Map.class).readValue(restResponse);
            assertTrue(deserializedAboutUs.get("name").equals("the-medusa-news"));

        } finally {
            session.logout();
        }
    }

    @Test
    public void test_myhippoproject_content_api() throws Exception {
        final Session session = createSession("admin", "admin");
        try {
            final Node medusaNews = session.getNode("/unittestcontent/documents/myhippoproject/news/2015/12/the-medusa-news");

            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "onehippo.io", "/myhippoproject/documents/" + medusaNews.getIdentifier(), null);
            final MockHttpServletResponse response = render(requestResponse);
            final String restResponse = response.getContentAsString();
            // TODO assertions
        } catch (Exception e) {
            log.error("error : ",e);
        } finally {
            session.logout();
        }
    }

    @Test
    public void test_search_result_contains_handle_uuids() throws Exception {
        Session liveUser = createLiveUserSession();
        try {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "onehippo.io", "/myhippoproject/documents/", null);

            final MockHttpServletResponse response = render(requestResponse);
            final String restResponse = response.getContentAsString();
            final Map<String, Object> searchResult = mapper.reader(Map.class).readValue(restResponse);

            final List<Map<String, Object>> itemsList = getItemsFromSearchResult(searchResult);
            for (Map<String, Object> item : itemsList) {
                final Node node = liveUser.getNodeByIdentifier((String)item.get("id"));
                assertTrue(node.isNodeType(NT_HANDLE));
            }
        } finally {
            if (liveUser != null) {
                liveUser.logout();
            }
        }
    }

    @Test
    public void test_search_result_is_ordered_by_default_on_publication_date_descending() throws Exception {
        Session liveUser = createLiveUserSession();
        try {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "onehippo.io", "/myhippoproject/documents/", null);

            final MockHttpServletResponse response = render(requestResponse);
            final String restResponse = response.getContentAsString();

            final Map<String, Object> searchResult = mapper.reader(Map.class).readValue(restResponse);

            final List<Map<String, Object>> itemsList = getItemsFromSearchResult(searchResult);

            int processed = 0;
            Calendar prev = null;
            for (Map<String, Object> item : itemsList) {
                final Node handleNode = liveUser.getNodeByIdentifier((String)item.get("id"));
                final Node node = handleNode.getNode(handleNode.getName());
                if (!node.hasProperty(HIPPOSTDPUBWF_PUBLICATION_DATE)) {
                    continue;
                }
                final Calendar date = node.getProperty(HIPPOSTDPUBWF_PUBLICATION_DATE).getDate();
                if (prev != null) {
                    assertTrue(prev.after(date) || prev.equals(date));
                }
                prev = date;
                processed++;
            }

            assertTrue("There must be at least two results to be able to compare sort order.", processed > 1);

        } finally {
            if (liveUser != null) {
                liveUser.logout();
            }
        }
    }

    @Test
    public void test_search_result_is_ordered_by_one_sort_parameter_default_sortorder() throws Exception {
        Session liveUser = createLiveUserSession();
        try {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "onehippo.io", "/myhippoproject/documents/", "_orderBy=" + PROPERTY_TITLE);

            final MockHttpServletResponse response = render(requestResponse);
            final String restResponse = response.getContentAsString();

            final Map<String, Object> searchResult = mapper.reader(Map.class).readValue(restResponse);

            final List<Map<String, Object>> itemsList = getItemsFromSearchResult(searchResult);

            int processed = 0;
            String previous = null;
            for (Map<String, Object> item : itemsList) {
                final Node handleNode = liveUser.getNodeByIdentifier((String) item.get("id"));
                final Node node = handleNode.getNode(handleNode.getName());
                if (!node.hasProperty(PROPERTY_TITLE) && !node.hasProperty(PROPERTY_DATE)) {
                    continue;
                }
                String current = node.getProperty(PROPERTY_TITLE).getString();
                if (previous != null) {
                    assertTrue(previous.compareTo(current) > 0); // default order is descending
                }
                previous = current;
                processed++;
            }

            assertTrue("There must be at least two results to be able to compare sort order.", processed > 1);

        } finally {
            if (liveUser != null) {
                liveUser.logout();
            }
        }
    }

    @Test
    public void test_search_result_is_ordered_by_one_sort_parameter_ascending_sortorder() throws Exception {
        Session liveUser = createLiveUserSession();
        try {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "onehippo.io", "/myhippoproject/documents/", "_orderBy=" + PROPERTY_TITLE + "&_sortOrder=ascending");

            final MockHttpServletResponse response = render(requestResponse);
            final String restResponse = response.getContentAsString();

            final Map<String, Object> searchResult = mapper.reader(Map.class).readValue(restResponse);

            final List<Map<String, Object>> itemsList = getItemsFromSearchResult(searchResult);

            int processed = 0;
            String previous = null;
            for (Map<String, Object> item : itemsList) {
                final Node handleNode = liveUser.getNodeByIdentifier((String) item.get("id"));
                final Node node = handleNode.getNode(handleNode.getName());
                if (!node.hasProperty(PROPERTY_TITLE)) {
                    continue;
                }
                String current = node.getProperty(PROPERTY_TITLE).getString();
                if (previous != null) {
                    assertTrue(previous.compareTo(current) < 0);
                }
                processed++;
                previous = current;
            }

            assertTrue("There must be at least two results to be able to compare sort order.", processed > 1);

        } finally {
            if (liveUser != null) {
                liveUser.logout();
            }
        }
    }

    @Test
    public void test_search_result_is_ordered_by_two_sort_parameters_with_different_sortorders() throws Exception {
        Session liveUser = createLiveUserSession();
        try {
            final RequestResponseMock requestResponse = mockGetRequestResponse(
                    "http", "onehippo.io", "/myhippoproject/documents/", "_orderBy=" + PROPERTY_DATE + "," + PROPERTY_TITLE + "&_sortOrder=ascending,descending");

            final MockHttpServletResponse response = render(requestResponse);
            final String restResponse = response.getContentAsString();

            final Map<String, Object> searchResult = mapper.reader(Map.class).readValue(restResponse);

            final List<Map<String, Object>> itemsList = getItemsFromSearchResult(searchResult);

            int processed = 0;
            String previousTitle = null;
            Calendar previousDate = null;
            for (Map<String, Object> item : itemsList) {
                final Node handleNode = liveUser.getNodeByIdentifier((String) item.get("id"));
                final Node node = handleNode.getNode(handleNode.getName());
                if (!node.hasProperty(PROPERTY_TITLE) && !node.hasProperty(PROPERTY_DATE)) {
                    continue;
                }
                final String currentTitle = node.getProperty(PROPERTY_TITLE).getString();
                final Calendar currentDate = node.getProperty(PROPERTY_DATE).getDate();

                if (previousDate != null && previousTitle != null) {
                    if(previousDate.equals(currentDate)) {
                        assertTrue(previousTitle.compareTo(currentTitle) > 0);
                        processed++;
                    }
                }
                previousDate = currentDate;
                previousTitle = currentTitle;
            }

            assertTrue("At least one comparison must have been made on the second property.", processed > 0);

        } finally {
            if (liveUser != null) {
                liveUser.logout();
            }
        }
    }

    @Test
    public void test_document_attribute_selection() throws Exception {
        // about-us  handle identifier is 'ebebebeb-5fa8-48a8-b03b-4524373d992b'
        RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/api/documents/30092f4e-2ef7-4c72-86a5-8ce895908937", "_attributes=unittestproject:summary,unittestproject:title");

        Map<String, Object> items = renderAndGetDocumentItems(requestResponse);
        assertNotNull(items);
        assertEquals(2, items.size());
        assertEquals("Summary of the about us", items.get("unittestproject:summary"));
        assertEquals("This is the about us", items.get("unittestproject:title"));

        requestResponse = mockGetRequestResponse(
                "http", "localhost", "/api/documents/30092f4e-2ef7-4c72-86a5-8ce895908937", "_attributes=no-such-property");

        items = renderAndGetDocumentItems(requestResponse);
        // TODO - this should never be null imho
        assertNull(items);
    }

    @Test
    public void test_list_attribute_selection() throws Exception {
        RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/api/documents/",
                "_attributes=unittestproject:summary,unittestproject:title&_nodetype=unittestproject:textpage");

        List<Map<String, Object>> listItems = renderAndGetListItems(requestResponse);
        assertTrue(listItems.size() > 0);

        for (Map<String, Object> item : listItems) {
            final Map<String, Object> documentItems = (Map<String, Object>) item.get("items");
            assertEquals(2, documentItems.keySet().size());
            assertNotNull(documentItems.get("unittestproject:title"));
            assertNotNull(documentItems.get("unittestproject:summary"));
        }

        requestResponse = mockGetRequestResponse("http", "localhost", "/api/documents/", null);

        listItems = renderAndGetListItems(requestResponse);
        assertTrue(listItems.size() > 0);

        for (Map<String, Object> item : listItems) {
            final Map<String, Object> documentItems = (Map<String, Object>) item.get("items");
            assertTrue(documentItems.keySet().size() > 2);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> renderAndGetDocumentItems(final RequestResponseMock requestResponse) throws Exception {
        MockHttpServletResponse response = render(requestResponse);

        String restResponse = response.getContentAsString();
        assertTrue(StringUtils.isNotEmpty(restResponse));

        Map<String, Object> deserializedResponse = mapper.reader(Map.class).readValue(restResponse);
        return (Map<String, Object>) deserializedResponse.get("items");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> renderAndGetListItems(final RequestResponseMock requestResponse) throws Exception {
        MockHttpServletResponse response = render(requestResponse);

        String restResponse = response.getContentAsString();
        assertTrue(StringUtils.isNotEmpty(restResponse));

        Map<String, Object> deserializedResponse = mapper.reader(Map.class).readValue(restResponse);
        return (List) deserializedResponse.get("items");
    }

    private List<Map<String, Object>> getItemsFromSearchResult(final Map<String, Object> searchResult) {
        final Object items = searchResult.get("items");
        assertNotNull(items);
        assertTrue(items instanceof List);
        return (List)items;
    }
}
