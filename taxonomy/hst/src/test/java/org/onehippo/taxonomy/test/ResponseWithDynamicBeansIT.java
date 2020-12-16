/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.test;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.pagemodelapi.common.AbstractPageModelApiITCases;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResponseWithDynamicBeansIT extends AbstractPageModelApiITCases {

    public static final String EXTRA_ANNOTATED_CLASSES_CONFIGURATION_PARAM = "classpath*:org/onehippo/taxonomy/contentbean/**/*.class";

    /**
     * addAnnotatedClassesConfigurationParam must be added before super setUpClass, hence redefine same setUpClass method
     * to hide the super.setUpClass and invoke that explicitly
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractPageModelApiITCases.addAnnotatedClassesConfigurationParam(EXTRA_ANNOTATED_CLASSES_CONFIGURATION_PARAM);
        AbstractPageModelApiITCases.setUpClass();
    }

    @Test
    public void test_pagemodelapi__dynamic_beans__document_with_taxonomy() throws Exception {

        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/spa/resourceapi/genericdetail/dynamiccontent", null);

        final MockHttpServletResponse response = render(requestResponse);
        final String restResponse = response.getContentAsString();
        assertTrue("PageModelAPI response is empty", StringUtils.isNotEmpty(restResponse));

        final JsonNode root = mapper.readTree(restResponse);
        //Content is referenced in this json response by the property page.models.document.$ref, example format: "/content/uf227192b3c0941a196b1bee660626033"
        final String contentRef = root.path("page").path("models").path("document").path("$ref").asText();
        final String contentUuid = contentRef.substring(contentRef.lastIndexOf('/') + 1);

        final JsonNode contentNode = root.path("content").path(contentUuid);

        //Check taxonomy fields exist
        assertTrue("Taxonomy field with name 'taxonomyClassificationField' not found", contentNode.hasNonNull("taxonomyClassificationField"));
        assertTrue("Taxonomy field with name 'taxonomyClassificationField2' not found", contentNode.hasNonNull("taxonomyClassificationField2"));
        assertTrue("Taxonomy field with name 'taxonomyClassificationField3' not found", contentNode.hasNonNull("taxonomyClassificationField3"));

        //Value for taxonomy field in single selection mode
        final JsonNode taxonomyField = contentNode.path("taxonomyClassificationField");
        assertEquals("Field 'taxonomyClassificationField' does not contain expected taxonomy name", "Taxxa", taxonomyField.path("taxonomyName").asText());
        final JsonNode taxonomyClassificationValue = taxonomyField.path("taxonomyValues").iterator().next();
        assertEquals("Field 'taxonomyClassificationField' does not contain expected value", "stats", taxonomyClassificationValue.path("key").asText());
        assertEquals("Field 'taxonomyClassificationField' does not contain expected value", "Stats", taxonomyClassificationValue.path("label").asText());

        //Values for taxonomy field in multiple selection mode
        final JsonNode taxonomyField2 = contentNode.path("taxonomyClassificationField2");
        assertEquals("Field 'taxonomyClassificationField2' does not contain expected taxonomy name", "Taxxa", taxonomyField2.path("taxonomyName").asText());
        final Iterator<JsonNode> taxonomyClassificationValue2 = taxonomyField2.path("taxonomyValues").iterator();
        assertEquals("Field 'taxonomyClassificationField2' does not contain expected value", "stats", taxonomyClassificationValue2.next().path("key").asText());
        final JsonNode node = taxonomyClassificationValue2.next();
        assertEquals("Field 'taxonomyClassificationField2' does not contain expected value", "tasks", node.path("key").asText());

        //Path values for taxonomy field in multiple selection mode
        assertEquals("KeyPath of 'taxonomyClassificationField2' does not contain expected value", "0/tasks/",
                node.path("keyPath").asText());
        assertEquals("LabelPath of 'taxonomyClassificationField2' does not contain expected value", "0/Tasks/",
                node.path("labelPath").asText());
        final JsonNode inner1Task = taxonomyClassificationValue2.next();
        assertEquals("KeyPath of 'taxonomyClassificationField2' does not contain expected value", "1/tasks/tasklevel1/",
                inner1Task.path("keyPath").asText());
        assertEquals("LabelPath of 'taxonomyClassificationField2' does not contain expected value",
                "1/Tasks/Task Level 1/", inner1Task.path("labelPath").asText());
        final JsonNode inner2Task = taxonomyClassificationValue2.next();
        assertEquals("KeyPath of 'taxonomyClassificationField2' does not contain expected value",
                "2/tasks/tasklevel1/tasklevel2/", inner2Task.path("keyPath").asText());
        assertEquals("LabelPath of 'taxonomyClassificationField2' does not contain expected value",
                "2/Tasks/Task Level 1/Task Level 2/", inner2Task.path("labelPath").asText());

        //Values for taxonomy field associated with different taxonomy, in multiple selection mode
        final JsonNode taxonomyField3 = contentNode.path("taxonomyClassificationField3");
        assertEquals("Field 'taxonomyClassificationField3' does not contain expected taxonomy name", "Taxxa2", taxonomyField3.path("taxonomyName").asText());
        final Iterator<JsonNode> taxonomyClassificationValue3 = taxonomyField3.path("taxonomyValues").iterator();
        assertEquals("Field 'taxonomyClassificationField3' does not contain expected value", "stats2", taxonomyClassificationValue3.next().path("key").asText());
        final JsonNode taxonomyValue3SecondEntry = taxonomyClassificationValue3.next();
        assertEquals("Field 'taxonomyClassificationField3' does not contain expected value", "tasks2", taxonomyValue3SecondEntry.path("key").asText());
        assertEquals("Field 'taxonomyClassificationField3' does not contain expected value", "Tasks2", taxonomyValue3SecondEntry.path("label").asText());

        //TODO Add a case for a misconfigured field, to test exception handling, e.g
        // empty taxonomy name in cluster options
        // field not found under nodetype
        // editor template not found or misconfigured 'field' property
    }

}
