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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.BaseDocument;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoResourceBean;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * Tests the dynamic bean service for all primitive and compound fields. 
 * The test for 'Value list item' compound field is not added because both BeanWriter 
 * tool and dynamic bean feature don't support this field. 
 */
public class TestDynamicBeanService extends AbstractBeanTestCase {

    private static final String TEST_DOCUMENT_TYPE_CONTENTS_PATH = "/unittestcontent/documents/unittestproject/common/dynamiccontent/dynamiccontent";

    private static final String BOOLEAN_TYPE_FIELD_NAME = "getBooleanTypeField";
    private static final String BOOLEAN_RADIO_GROUP_TYPE_FIELD_NAME = "getBooleanRadioGroupTypeField";
    private static final String CALENDER_DATE_TYPE_FIELD_NAME = "getCalendardateTypeField";
    private static final String DATE_TYPE_FIELD_NAME = "getDateTypeField";
    private static final String DECIMAL_NUMBER_TYPE_FIELD_NAME = "getDoubleTypeField";
    private static final String DOCBASE_TYPE_FIELD_NAME = "getDocbaseTypeField";
    private static final String DYNAMIC_DROPDOWN_TYPE_FIELD_NAME = "getDynamicdropdownTypeField";
    private static final String INTEGER_NUMBER_TYPE_FIELD_NAME = "getLongTypeField";
    private static final String HTML_TYPE_FIELD_NAME = "getHtmlTypeField";
    private static final String RADIO_GROUP_TYPE_FIELD_NAME = "getRadioGroupTypeField";
    private static final String STATIC_DROPDOWN_TYPE_FIELD_NAME = "getStaticdropdownTypeField";
    private static final String STRING_TYPE_FIELD_NAME = "getStringTypeField";
    private static final String TEXT_TYPE_FIELD_NAME = "getTextTypeField";

    private static final String IMAGE_LINK_COMPOUND_TYPE_FIELD_NAME = "getImagelinkCompoundType";
    private static final String LINK_COMPOUND_TYPE_FIELD_NAME = "getMirrorCompoundType";
    private static final String RESOURCE_COMPOUND_TYPE_FIELD_NAME = "getResourceCompoundType";
    private static final String RICH_TEXT_EDITOR_COMPOUND_TYPE_FIELD_NAME = "getRichTextEditorCompoundType";

    private ObjectConverter objectConverter;

    private DateFormat dateParser;

    protected List<Class<? extends HippoBean>> annotatedClasses;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        if (annotatedClasses == null) {
            annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
            annotatedClasses.add(BaseDocument.class);
        }
        objectConverter = getObjectConverter(annotatedClasses);

        dateParser = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);

    }

    protected Object getContentBean() throws Exception {
        Object generatedBean = objectConverter.getObject(session, TEST_DOCUMENT_TYPE_CONTENTS_PATH);

        assertNotNull("The content bean is not created for " + TEST_DOCUMENT_TYPE_CONTENTS_PATH, generatedBean);
        assertThat(generatedBean, instanceOf(HippoBean.class));

        return generatedBean;
    }

    @SuppressWarnings("unchecked")
    protected <T> T callContentBeanMethod(Object generatedBean, String methodName, Class<T> returnType) throws Exception {
        Method method = generatedBean.getClass().getMethod(methodName, null);

        assertNotNull("The method '" + methodName + "' is not found", method);
        assertEquals(returnType, method.getReturnType());

        return (T) method.invoke(generatedBean, null);
    }

    @Test
    public void testGetValueOfStringTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        String value = callContentBeanMethod(generatedBean, STRING_TYPE_FIELD_NAME, String.class);

        assertNotNull("The method '" + STRING_TYPE_FIELD_NAME + "' didn't return any value", value);
        assertEquals("string Value", value);
    }

    @Test
    public void testGetValueOfTextTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        String value = callContentBeanMethod(generatedBean, TEXT_TYPE_FIELD_NAME, String.class);

        assertNotNull("The method '" + TEXT_TYPE_FIELD_NAME + "' didn't return any value", value);
        assertEquals("textvalue", value);
    }

    @Test
    public void testGetValueOfIntegerNumberTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Long value = callContentBeanMethod(generatedBean, INTEGER_NUMBER_TYPE_FIELD_NAME, Long.class);

        assertNotNull("The method '" + INTEGER_NUMBER_TYPE_FIELD_NAME + "' didn't return any value", value);
        assertEquals(new Long(50), value);
    }

    @Test
    public void testGetValueOfDecimalNumberTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Double value = callContentBeanMethod(generatedBean, DECIMAL_NUMBER_TYPE_FIELD_NAME, Double.class);

        assertNotNull("The method '" + DECIMAL_NUMBER_TYPE_FIELD_NAME + "' didn't return any value", value);
        assertEquals(new Double(100), value);
    }

    @Test
    public void testGetValueOfBooleanTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Boolean value = callContentBeanMethod(generatedBean, BOOLEAN_TYPE_FIELD_NAME, Boolean.class);

        assertNotNull("The method '" + BOOLEAN_TYPE_FIELD_NAME + "' didn't return any value", value);
        assertFalse(value);
    }

    @Test
    public void testGetValueOfDateTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Calendar value = callContentBeanMethod(generatedBean, DATE_TYPE_FIELD_NAME, Calendar.class);

        assertNotNull("The method '" + DATE_TYPE_FIELD_NAME + "' didn't return any value", value);

        Date result = dateParser.parse("25/03/2019");
        assertEquals(result, value.getTime());
    }

    @Test
    public void testGetValueOfCalenderDateTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Calendar value = callContentBeanMethod(generatedBean, CALENDER_DATE_TYPE_FIELD_NAME, Calendar.class);

        assertNotNull("The method '" + CALENDER_DATE_TYPE_FIELD_NAME + "' didn't return any value", value);

        Date result = dateParser.parse("25/03/2019");
        assertEquals(result, value.getTime());
    }

    @Test
    public void testGetValueOfHtmlTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        String value = callContentBeanMethod(generatedBean, HTML_TYPE_FIELD_NAME, String.class);

        assertNotNull("The method '" + HTML_TYPE_FIELD_NAME + "' didn't return any value", value);
        assertEquals("htmltypecontent", value);
    }

    @Test
    public void testGetContentOfImageLinkCompoundTypeWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        HippoGalleryImageSet hippoGalleryImageSet = callContentBeanMethod(generatedBean, IMAGE_LINK_COMPOUND_TYPE_FIELD_NAME,
                HippoGalleryImageSet.class);

        assertNotNull("The method '" + IMAGE_LINK_COMPOUND_TYPE_FIELD_NAME + "' didn't return any value", hippoGalleryImageSet);

        if (hippoGalleryImageSet != null) {
            assertEquals("db02dde5-0098-4488-a72c-2a4fc6d51beb", hippoGalleryImageSet.getNode().getParent().getIdentifier());
        }
    }

    @Test
    public void testGetContentOfRichTextEditorCompoundTypeWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        HippoHtml hippoHtml = callContentBeanMethod(generatedBean, RICH_TEXT_EDITOR_COMPOUND_TYPE_FIELD_NAME, HippoHtml.class);

        assertNotNull("The method '" + RICH_TEXT_EDITOR_COMPOUND_TYPE_FIELD_NAME + "' didn't return any value", hippoHtml);

        if (hippoHtml != null) {
            assertEquals("richtexteditorcontent", hippoHtml.getContent());
        }
    }

    @Test
    public void testGetContentOfDocbaseTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        HippoBean value = callContentBeanMethod(generatedBean, DOCBASE_TYPE_FIELD_NAME, HippoBean.class);

        assertNotNull("The method '" + DOCBASE_TYPE_FIELD_NAME + "' didn't return any value", value);

        if (value != null) {
            assertEquals("30092f4e-2ef7-4c72-86a5-8ce895908937", value.getNode().getProperty("hippo:related").getValues()[0].getString());
        }

    }

    @Test
    public void testGetValueOfDynamicdropdownTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        String value = callContentBeanMethod(generatedBean, DYNAMIC_DROPDOWN_TYPE_FIELD_NAME, String.class);

        assertNotNull("The method '" + DYNAMIC_DROPDOWN_TYPE_FIELD_NAME + "' didn't return any value", value);

        assertEquals("dynamicvalue", value);
    }

    @Test
    public void testGetValueOfRadioGroupTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        String value = callContentBeanMethod(generatedBean, RADIO_GROUP_TYPE_FIELD_NAME, String.class);

        assertNotNull("The method '" + RADIO_GROUP_TYPE_FIELD_NAME + "' didn't return any value", value);

        assertEquals("radiogroupvalue", value);
    }

    @Test
    public void testGetValueOfBooleanRadioGroupTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Boolean value = callContentBeanMethod(generatedBean, BOOLEAN_RADIO_GROUP_TYPE_FIELD_NAME, Boolean.class);

        assertNotNull("The method '" + BOOLEAN_RADIO_GROUP_TYPE_FIELD_NAME + "' didn't return any value", value);

        assertFalse(value);
    }

    @Test
    public void testGetValueOfStaticdropdownTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        String value = callContentBeanMethod(generatedBean, STATIC_DROPDOWN_TYPE_FIELD_NAME, String.class);

        assertNotNull("The method '" + STATIC_DROPDOWN_TYPE_FIELD_NAME + "' didn't return any value", value);

        assertEquals("staticvalue", value);
    }

    @Test
    public void testGetContentOfLinkCompoundTypeWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        HippoBean hippoBean = callContentBeanMethod(generatedBean, LINK_COMPOUND_TYPE_FIELD_NAME, HippoBean.class);

        assertNotNull("The method '" + LINK_COMPOUND_TYPE_FIELD_NAME + "' didn't return any value", hippoBean);

        if (hippoBean != null) {
            assertEquals("30092f4e-2ef7-4c72-86a5-8ce895908937", hippoBean.getNode().getParent().getIdentifier());
        }
    }

    @Test
    public void testGetContentOfResourceCompoundTypeWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        HippoResourceBean hippoResourceBean = callContentBeanMethod(generatedBean, RESOURCE_COMPOUND_TYPE_FIELD_NAME, HippoResourceBean.class);

        assertNotNull("The method '" + RESOURCE_COMPOUND_TYPE_FIELD_NAME + "' didn't return any value", hippoResourceBean);

        if (hippoResourceBean != null) {
            assertEquals("picture_thumbnail.jpeg", hippoResourceBean.getFilename());
        }
    }

}
