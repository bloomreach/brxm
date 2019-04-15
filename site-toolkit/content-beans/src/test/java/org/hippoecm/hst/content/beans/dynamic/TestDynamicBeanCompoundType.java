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
import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.BaseDocument;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * 
 * Tests the dynamic bean service for custom compound types 
 */
public class TestDynamicBeanCompoundType extends AbstractBeanTestCase {

    private static final String TEST_DOCUMENT_TYPE_CONTENTS_PATH = "/content/documents/contentbeanstest/content/dynamicdocumentcontent/dynamicdocumentcontent";

    private static final String CUSTOM_COMPOUND_TYPE_METHOD_NAME = "getDynamiccompound";
    private static final String CUSTOM_COMPOUND_TYPE_METHOD_NAME_WITH_RETURN_LIST = "getDynamiccompoundmultiple";

    private static final String STRING_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS = "getStringTypeField";
    private static final String RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS = "getRichTextEditorCompoundType";

    private static final String INNER_CUSTOM_COMPOUND_TYPE_METHOD_NAME = "getDynamicinnercompound";
    private static final String STRING_TYPE_METHOD_NAME_IN_INNER_CUSTOM_COMPOUND_CLASS = "getStringTypeField";

    private ObjectConverter objectConverter;

    protected List<Class<? extends HippoBean>> annotatedClasses;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        if (annotatedClasses == null) {
            annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
            annotatedClasses.add(BaseDocument.class);
        }
        objectConverter = getObjectConverter(annotatedClasses);

    }

    private Object getContentBean() throws Exception {
        Object generatedBean = objectConverter.getObject(session, TEST_DOCUMENT_TYPE_CONTENTS_PATH);

        assertNotNull("The content bean is not created for " + TEST_DOCUMENT_TYPE_CONTENTS_PATH, generatedBean);
        assertThat(generatedBean, instanceOf(HippoBean.class));

        return generatedBean;
    }

    @SuppressWarnings("unchecked")
    private <T> T callContentBeanMethod(Object generatedBean, String methodName, Class<T> returnType) throws Exception {
        Method method = generatedBean.getClass().getMethod(methodName);

        assertNotNull("The method '" + methodName + "' is not found", method);
        assertEquals(returnType, method.getReturnType());

        return (T) method.invoke(generatedBean);
    }

    @SuppressWarnings("unchecked")
    private <T> T callCustomCompoundTypeMethod(Object generatedBean, String methodName, boolean multiple) throws Exception {
        Method method = generatedBean.getClass().getMethod(methodName);

        assertNotNull("The method '" + methodName + "' is not found", method);

        T value = (T) method.invoke(generatedBean);

        assertThat(value, multiple ? instanceOf(List.class) : instanceOf(HippoCompound.class));

        return value;
    }

    @Test
    public void testGetValueOfStringTypeFieldInCustomCompoundTypeFromDocumentBeanClass() throws Exception {

        Object generatedBean = getContentBean();

        HippoCompound hippoCompound = callCustomCompoundTypeMethod(generatedBean, CUSTOM_COMPOUND_TYPE_METHOD_NAME, false);

        assertNotNull("The method '" + CUSTOM_COMPOUND_TYPE_METHOD_NAME + "' didn't return any value", hippoCompound);

        String result = callContentBeanMethod(hippoCompound, STRING_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS, String.class);

        assertNotNull("The method '" + STRING_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS + "' didn't return any value", hippoCompound);
        assertEquals("alexanderkade", result);
        assertNotNull(generatedBean);
    }

    @Test
    public void testGetContentOfRichTextEditorTypeFieldInCustomCompoundTypeFromDocumentBeanClass() throws Exception {

        Object generatedBean = getContentBean();

        HippoCompound hippoCompound = callCustomCompoundTypeMethod(generatedBean, CUSTOM_COMPOUND_TYPE_METHOD_NAME, false);

        assertNotNull("The method '" + CUSTOM_COMPOUND_TYPE_METHOD_NAME + "' didn't return any value", hippoCompound);

        HippoHtml hippoHtml = callContentBeanMethod(hippoCompound, RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS, HippoHtml.class);

        assertNotNull("The method '" + RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS + "' didn't return any value", hippoHtml);
        assertEquals("contentOfHtmlFiledInCompoundTypeClass", hippoHtml.getContent());
        assertNotNull(generatedBean);
    }

    @Test
    public void testGetContentOfRichTextEditorTypeFieldInMultipleCustomCompoundTypeFromDocumentBeanClass() throws Exception {

        Object generatedBean = getContentBean();

        List<HippoCompound> hippoCompounds = callCustomCompoundTypeMethod(generatedBean, CUSTOM_COMPOUND_TYPE_METHOD_NAME_WITH_RETURN_LIST, true);

        assertNotNull("The method '" + CUSTOM_COMPOUND_TYPE_METHOD_NAME_WITH_RETURN_LIST + "' didn't return any value", hippoCompounds);

        HippoHtml hippoHtml = callContentBeanMethod(hippoCompounds.get(0), RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS, HippoHtml.class);

        assertNotNull("The method '" + RICH_TEXT_EDITOR_TYPE_METHOD_NAME_IN_CUSTOM_COMPOUND_CLASS + "' didn't return any value", hippoHtml);
        assertEquals("multipleListContent", hippoHtml.getContent());
        assertNotNull(generatedBean);
    }

    @Test
    public void testGetValueOfStringTypeFieldInCustomCompoundTypeInCustomCompoundTypeFromDocumentBeanClass() throws Exception {

        Object generatedBean = getContentBean();

        HippoCompound hippoCompound = callCustomCompoundTypeMethod(generatedBean, CUSTOM_COMPOUND_TYPE_METHOD_NAME, false);

        assertNotNull("The method '" + CUSTOM_COMPOUND_TYPE_METHOD_NAME + "' didn't return any value", hippoCompound);

        HippoCompound insideCompound = callCustomCompoundTypeMethod(hippoCompound, INNER_CUSTOM_COMPOUND_TYPE_METHOD_NAME, false);

        assertNotNull("The method '" + INNER_CUSTOM_COMPOUND_TYPE_METHOD_NAME + "' didn't return any value", insideCompound);

        String result = callContentBeanMethod(insideCompound, STRING_TYPE_METHOD_NAME_IN_INNER_CUSTOM_COMPOUND_CLASS, String.class);

        assertNotNull("The method '" + STRING_TYPE_METHOD_NAME_IN_INNER_CUSTOM_COMPOUND_CLASS + "' didn't return any value", insideCompound);
        assertEquals("http://test.com", result);
        assertNotNull(generatedBean);

    }
}
