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

import java.util.ArrayList;

import org.hippoecm.hst.content.beans.BaseDocument;
import org.hippoecm.hst.content.beans.DynamicBeanPage;
import org.hippoecm.hst.content.beans.DynamicCompoundBean;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 
 * Tests the dynamic bean service for all primitive and compound fields with enhancing bean functionality.
 * For this, {@link DynamicBeanPage} content bean is marked with {@link org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated}.
 *
 */
public class TestDynamicExistingBean extends TestDynamicBeanService {

    private static final String TEST_DOCUMENT_TYPE_CONTENTS_PATH = "/content/documents/contentbeanstest/content/dynamiccontent/dynamiccontent";

    private static final String GET_COMPOUND_TYPE_METHOD_NAME = "getStandardCompoundType";
    private static final String CUSTOM_METHOD_NAME = "getDoubleTypeFieldMultipleByFive";
    private static final String INTEGER_NUMBER_TYPE_METHOD_NAME = "getLongTypeField2";

    public String getDocumentPath() {
        return TEST_DOCUMENT_TYPE_CONTENTS_PATH;
    }

    @Before
    public void setUp() throws Exception {
        annotatedClasses = new ArrayList<>();
        annotatedClasses.add(BaseDocument.class);
        annotatedClasses.add(DynamicBeanPage.class);
        annotatedClasses.add(DynamicCompoundBean.class);

        super.setUp();
    }

    @Test
    public void testDynamicCompoundBeanExtendsExistingCompoundBean() throws Exception {

        Object generatedBean = objectConverter.getObject(session, TEST_DOCUMENT_TYPE_CONTENTS_PATH);

        Object dynamicCompoundBean = generatedBean.getClass().getMethod(GET_COMPOUND_TYPE_METHOD_NAME).invoke(generatedBean);

        assertEquals(dynamicCompoundBean.getClass().getSuperclass(), DynamicCompoundBean.class);
    }

    @Test
    public void testDynamicDocumentBeanExtendsExistingDocumentBean() throws Exception {

        Object generatedBean = objectConverter.getObject(session, TEST_DOCUMENT_TYPE_CONTENTS_PATH);

        assertEquals(generatedBean.getClass().getSuperclass(), DynamicBeanPage.class);
    }

    @Test
    public void testGetValueOfDoubleTypeFieldMultipleByFiveMethod() throws Exception {

        Object generatedBean = getContentBean();

        Double value = callContentBeanMethod(generatedBean, CUSTOM_METHOD_NAME, Double.class);

        assertNotNull("The method '" + CUSTOM_METHOD_NAME + "' didn't return any value", value);
        assertEquals(new Double(500), value);
    }

    @Test
    public void testGetValueOfIntegerNumberTypeFieldFromExistingMethodInBeanClass() throws Exception {

        Object generatedBean = getContentBean();

        Long value = callContentBeanMethod(generatedBean, INTEGER_NUMBER_TYPE_METHOD_NAME, Long.class);

        assertNotNull("The method '" + INTEGER_NUMBER_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertEquals(new Long(400), value);
    }
    
}
