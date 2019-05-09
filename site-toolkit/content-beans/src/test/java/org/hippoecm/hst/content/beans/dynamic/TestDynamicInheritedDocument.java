/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.reflect.Method;
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * 
 * Tests the dynamic bean service for document type inheritance 
 */
public class TestDynamicInheritedDocument extends AbstractDynamicBeanServiceTest {

    private static final String TEST_SUB_DOCUMENT_TYPE_CONTENTS_PATH = "/content/documents/contentbeanstest/content/dynamicsubdocumentpage/dynamicsubdocumentpage";

    private static final String CUSTOM_COMPOUND_TYPE_METHOD_NAME_WITH_RETURN_LIST = "getDynamiccompoundmultiple";
    private static final String SUB_CUSTOM_COMPOUND_TYPE_METHOD_NAME  = "getDynamicsubcompound";

    private static final String RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_SUB_DOCUMENT_CLASS = "getSubrichtexteditor";

    private static final String RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS = "getRichTextEditorCompoundType";
    private static final String STRING_TYPE_METHOD_NAME_IN_SUB_CUSTOM_COMPOUND_CLASS = "getStringTypeField2";

    public String getDocumentPath() {
        return TEST_SUB_DOCUMENT_TYPE_CONTENTS_PATH;
    }

    @SuppressWarnings("unchecked")
    protected <T> T callCustomCompoundTypeMethod(Object generatedBean, String methodName, boolean multiple) throws Exception {
        Method method = generatedBean.getClass().getMethod(methodName);

        assertNotNull("The method '" + methodName + "' is not found", method);

        T value = (T) method.invoke(generatedBean);

        assertThat(value, multiple ? instanceOf(List.class) : instanceOf(HippoCompound.class));

        return value;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetContentOfRichTextEditorTypeFieldInSubDocumentTypeBeanClass() throws Exception {

        Object generatedBean = getContentBean();

        List<HippoHtml> hippoHtmls = callContentBeanMethod(generatedBean, RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_SUB_DOCUMENT_CLASS, List.class);

        assertNotNull("The method '" + RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_SUB_DOCUMENT_CLASS + "' didn't return any value", hippoHtmls);
        assertEquals("richTextEditorInSubdocument", hippoHtmls.get(0).getContent());
    }

    @Test
    public void testGetContentOfRichTextEditorTypeFieldInCustomCompoundTypeInSubDocumentTypeBeanClass() throws Exception {

        Object generatedBean = getContentBean();

        List<HippoCompound> value = callCustomCompoundTypeMethod(generatedBean, CUSTOM_COMPOUND_TYPE_METHOD_NAME_WITH_RETURN_LIST, true);

        assertNotNull("The method '" + CUSTOM_COMPOUND_TYPE_METHOD_NAME_WITH_RETURN_LIST + "' didn't return any value", value);

        HippoHtml hippoHtml = callContentBeanMethod(value.get(0), RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS, HippoHtml.class);

        assertNotNull("The method '" + RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS + "' didn't return any value", hippoHtml);
        assertEquals("multipleListContent", hippoHtml.getContent());
        assertNotNull(generatedBean);
    }

    @Test
    public void testGetValuesOfStringTypeFieldInSubCustomCompoundTypeInSubDocumentTypeBeanClass() throws Exception {

        Object generatedBean = getContentBean();

        HippoCompound hippoCompound = callCustomCompoundTypeMethod(generatedBean, SUB_CUSTOM_COMPOUND_TYPE_METHOD_NAME, false);

        assertNotNull("The method '" + SUB_CUSTOM_COMPOUND_TYPE_METHOD_NAME + "' didn't return any value", hippoCompound);

        String value = callContentBeanMethod(hippoCompound, STRING_TYPE_METHOD_NAME_IN_SUB_CUSTOM_COMPOUND_CLASS, String.class);

        assertNotNull("The method '" + STRING_TYPE_METHOD_NAME_IN_SUB_CUSTOM_COMPOUND_CLASS + "' didn't return any value", value);
        assertEquals("subcompoundvalue", value);
        assertNotNull(generatedBean);
    }
}
