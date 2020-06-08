/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 *
 * Tests the dynamic bean service for aggregated types
 */
public class TestDynamicBeanAggregatedType extends AbstractDynamicBeanServiceTest {

    private static final String TEST_DOCUMENT_TYPE_CONTENTS_PATH = "/content/documents/contentbeanstest/content/dynamicaggregatedcontent/aggregateddocumentcontent";

    private static final String STRING_METHOD_NAME = "getStringField";
    private static final String EXTRA_TYPE_STRING_TYPE_METHOD_NAME = "getExtraTypeText";

    public String getDocumentPath() {
        return TEST_DOCUMENT_TYPE_CONTENTS_PATH;
    }

    @Test
    public void testGetContentOfAggregatedTypeWithoutContentBean() throws Exception {

        final Object generatedBean = getContentBean();

        final String extraTypeStringField = callContentBeanMethod(generatedBean, EXTRA_TYPE_STRING_TYPE_METHOD_NAME,
                String.class);
        final String aggregatedDocStringField = callContentBeanMethod(generatedBean, STRING_METHOD_NAME, String.class);

        assertNotNull("The method '" + EXTRA_TYPE_STRING_TYPE_METHOD_NAME + "' didn't return any value",
                extraTypeStringField);
        assertNotNull("The method '" + STRING_METHOD_NAME + "' didn't return any value", aggregatedDocStringField);
    }

}
