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
package org.hippoecm.hst.pagemodelapi.v09;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.pagemodelapi.common.AbstractPageModelApiTestCases;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResponseWithDynamicBeansIT extends AbstractPageModelApiTestCases {

    @Test
    public void test_pagemodelapi__dynamic_beans__document_with_contentblocks() throws Exception {

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/spa/resourceapi/genericdetail/dynamiccontent", null);

        final MockHttpServletResponse response = render(requestResponse);
        final String restResponse = response.getContentAsString();
        assertTrue("PageModelAPI response is empty", StringUtils.isNotEmpty(restResponse));

        JsonNode root = mapper.readTree(restResponse);
        //Content is referenced in this json response by the property page.models.document.$ref, example format: "/content/uf227192b3c0941a196b1bee660626033"
        String contentRef = root.path("page").path("models").path("document").path("$ref").asText();
        String contentUuid = contentRef.substring(contentRef.lastIndexOf('/') + 1);

        JsonNode contentNode = root.path("content").path(contentUuid);

        //Custom contentblocks field without contentblocksWithValidator
        assertTrue("Content blocks field with name 'contentblocks' not found", contentNode.hasNonNull("contentblocks"));
        assertEquals("Field 'contentblocks' does not contain expected value", "Welcome Home!",
                contentNode.path("contentblocks").iterator().next().path("text").path("value").asText());

        //Custom contentblocks field with contentblocksWithValidator
        assertTrue("Content blocks field with name 'contentblocksWithValidator' not found", contentNode.hasNonNull("contentblocksWithValidator"));
        assertEquals("Field 'contentblocksWithValidator' does not contain expected value",
                "Welcome Home with validator!", contentNode.path("contentblocksWithValidator").iterator().next().path("text").path("value").asText());

        //Essentials generated contentblocks fields
        assertTrue("Content blocks fields with name 'essentialsGeneratedContentblocks' not found", contentNode.hasNonNull("essentialsGeneratedContentblocks"));
        Iterator<JsonNode> contentBlocks = contentNode.path("essentialsGeneratedContentblocks").iterator();
        assertEquals("First field named 'essentialsGeneratedContentblocks' does not contain expected value", "Welcome Home with Essentials!", contentBlocks.next().path("text").path("value").asText());
        assertEquals("Second field named 'essentialsGeneratedContentblocks' does not contain expected value", "Welcome Home with Essentials and lazy loaded types!", contentBlocks.next().path("text").path("value").asText());

    }
}
