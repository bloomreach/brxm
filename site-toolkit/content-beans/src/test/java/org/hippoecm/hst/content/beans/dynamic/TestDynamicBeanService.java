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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.time.DateUtils;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoResourceBean;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * Tests the dynamic bean service for all primitive and compound fields.
 * The test for 'Value list item' compound field is not added because both BeanWriter
 * tool and dynamic bean feature don't support this field.
 */
public class TestDynamicBeanService extends AbstractDynamicBeanServiceTest {

    private static final String TEST_DOCUMENT_TYPE_CONTENTS_PATH = "/content/documents/contentbeanstest/content/dynamiccontent/dynamiccontent";

    private static final String BOOLEAN_TYPE_METHOD_NAME = "getBooleanTypeField";
    private static final String MULTIPLE_BOOLEAN_TYPE_METHOD_NAME = "getMultipleBooleanTypeField";
    private static final String SINGLE_VALUE_MULTIPLE_BOOLEAN_TYPE_METHOD_NAME = "getSingleValueMultipleBooleanTypeField";
    private static final String MULTIPLE_VALUE_SINGLE_BOOLEAN_TYPE_METHOD_NAME = "getMultipleValueSingleBooleanTypeField";
    private static final String CALENDAR_DATE_TYPE_METHOD_NAME = "getCalendarDateTypeField";
    private static final String DATE_TYPE_METHOD_NAME = "getDateTypeField";
    private static final String MULTIPLE_DATE_TYPE_METHOD_NAME = "getMultipleDateTypeField";
    private static final String SINGLE_VALUE_MULTIPLE_DATE_TYPE_METHOD_NAME = "getSingleValueMultipleDateTypeField";
    private static final String MULTIPLE_VALUE_SINGLE_DATE_TYPE_METHOD_NAME = "getMultipleValueSingleDateTypeField";
    private static final String DOUBLE_TYPE_METHOD_NAME = "getDoubleTypeField";
    private static final String MULTIPLE_DOUBLE_TYPE_METHOD_NAME = "getMultipleDoubleTypeField";
    private static final String SINGLE_VALUE_MULTIPLE_DOUBLE_TYPE_METHOD_NAME = "getSingleValueMultipleDoubleTypeField";
    private static final String MULTIPLE_VALUE_SINGLE_DOUBLE_TYPE_METHOD_NAME = "getMultipleValueSingleDoubleTypeField";
    private static final String DOCBASE_TYPE_METHOD_NAME = "getDocbaseTypeField";
    private static final String LONG_TYPE_METHOD_NAME = "getLongTypeField";
    private static final String MULTIPLE_LONG_TYPE_METHOD_NAME = "getMultipleLongTypeField";
    private static final String SINGLE_VALUE_MULTIPLE_LONG_TYPE_METHOD_NAME = "getSingleValueMultipleLongTypeField";
    private static final String MULTIPLE_VALUE_SINGLE_LONG_TYPE_METHOD_NAME = "getMultipleValueSingleLongTypeField";
    private static final String HTML_TYPE_METHOD_NAME = "getHtmlTypeField";
    private static final String STRING_TYPE_METHOD_NAME = "getStringTypeField";
    private static final String TEXT_TYPE_METHOD_NAME = "getTextTypeField";

    private static final String LINK_COMPOUND_TYPE_METHOD_NAME = "getMirrorCompoundType";
    private static final String RESOURCE_COMPOUND_TYPE_METHOD_NAME = "getResourceCompoundType";
    private static final String RICH_TEXT_EDITOR_COMPOUND_TYPE_METHOD_NAME = "getRichTextEditorCompoundType";
    private static final String CONTENT_BLOCKS_TYPE_METHOD_NAME = "getContentblocks";
    private static final String CONTENT_BLOCKS_TYPE_WITH_VALIDATOR_METHOD_NAME = "getContentblocksWithValidator";
    private static final String ESSENTIALS_GENERATED_CONTENT_BLOCKS_TYPE_METHOD_NAME = "getEssentialsGeneratedContentblocks";

    private DateFormat dateParser = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);

    public String getDocumentPath() {
        return TEST_DOCUMENT_TYPE_CONTENTS_PATH;
    }

    @Ignore // This test belongs to the improvement of CMS-11933
    @Test
    public void testGetValueOfStringTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        String value = callContentBeanMethod(generatedBean, STRING_TYPE_METHOD_NAME, String.class);

        assertNotNull("The method '" + STRING_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertEquals("string Value", value);
    }

    @Ignore // This test belongs to the improvement of CMS-11933
    @Test
    public void testGetValueOfTextTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        String value = callContentBeanMethod(generatedBean, TEXT_TYPE_METHOD_NAME, String.class);

        assertNotNull("The method '" + TEXT_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertEquals("textvalue", value);
    }

    @Test
    public void testGetValueOfLongTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Long value = callContentBeanMethod(generatedBean, LONG_TYPE_METHOD_NAME, Long.class);

        assertNotNull("The method '" + LONG_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertEquals(new Long(50), value);
    }

    @Test
    public void testGetValueOfMultipleLongTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Long[] value = callContentBeanMethod(generatedBean, MULTIPLE_LONG_TYPE_METHOD_NAME, Long[].class);

        assertNotNull("The method '" + MULTIPLE_LONG_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertThat(value.length, equalTo(2));
    }

    @Test
    public void testGetValueOfSingleValueMultipleLongTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Long[] value = callContentBeanMethod(generatedBean, SINGLE_VALUE_MULTIPLE_LONG_TYPE_METHOD_NAME, Long[].class);

        assertNotNull("The method '" + SINGLE_VALUE_MULTIPLE_LONG_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertThat(value.length, equalTo(1));
    }

    @Test
    public void testGetValueOfMultipleValueSingleLongTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Long value = callContentBeanMethod(generatedBean, MULTIPLE_VALUE_SINGLE_LONG_TYPE_METHOD_NAME, Long.class);

        assertNotNull("The method '" + MULTIPLE_VALUE_SINGLE_LONG_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertEquals(new Long(50), value);
    }

    @Test
    public void testGetValueOfDoubleTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Double value = callContentBeanMethod(generatedBean, DOUBLE_TYPE_METHOD_NAME, Double.class);

        assertNotNull("The method '" + DOUBLE_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertEquals(new Double(100), value);
    }

    @Test
    public void testGetValueOfMultipleDoubleTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Double[] value = callContentBeanMethod(generatedBean, MULTIPLE_DOUBLE_TYPE_METHOD_NAME, Double[].class);

        assertNotNull("The method '" + MULTIPLE_DOUBLE_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertThat(value.length, equalTo(2));
    }

    @Test
    public void testGetValueOfSingleValueMultipleDoubleTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Double[] value = callContentBeanMethod(generatedBean, SINGLE_VALUE_MULTIPLE_DOUBLE_TYPE_METHOD_NAME, Double[].class);

        assertNotNull("The method '" + SINGLE_VALUE_MULTIPLE_DOUBLE_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertThat(value.length, equalTo(1));
    }

    @Test
    public void testGetValueOfMultipleValueSingleDoubleTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Double value = callContentBeanMethod(generatedBean, MULTIPLE_VALUE_SINGLE_DOUBLE_TYPE_METHOD_NAME, Double.class);

        assertNotNull("The method '" + MULTIPLE_VALUE_SINGLE_DOUBLE_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertEquals(new Double(100), value);
    }

    @Test
    public void testGetValueOfBooleanTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Boolean value = callContentBeanMethod(generatedBean, BOOLEAN_TYPE_METHOD_NAME, Boolean.class);

        assertNotNull("The method '" + BOOLEAN_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertFalse(value);
    }

    @Test
    public void testGetValueOfMultipleBooleanTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Boolean[] value = callContentBeanMethod(generatedBean, MULTIPLE_BOOLEAN_TYPE_METHOD_NAME, Boolean[].class);

        assertNotNull("The method '" + MULTIPLE_BOOLEAN_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertThat(value.length, equalTo(2));
    }

    @Test
    public void testGetValueOfSingleValueMultipleBooleanTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Boolean[] value = callContentBeanMethod(generatedBean, SINGLE_VALUE_MULTIPLE_BOOLEAN_TYPE_METHOD_NAME, Boolean[].class);

        assertNotNull("The method '" + SINGLE_VALUE_MULTIPLE_BOOLEAN_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertThat(value.length, equalTo(1));
    }

    @Test
    public void testGetValueOfMultipleValueSingleBooleanTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Boolean value = callContentBeanMethod(generatedBean, MULTIPLE_VALUE_SINGLE_BOOLEAN_TYPE_METHOD_NAME, Boolean.class);

        assertNotNull("The method '" + MULTIPLE_VALUE_SINGLE_BOOLEAN_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertFalse(value);
    }

    @Test
    public void testGetValueOfDateTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Calendar value = callContentBeanMethod(generatedBean, DATE_TYPE_METHOD_NAME, Calendar.class);

        assertNotNull("The method '" + DATE_TYPE_METHOD_NAME + "' didn't return any value", value);

        Date result = dateParser.parse("25/03/2019");
        assertTrue(DateUtils.isSameDay(result, value.getTime()));
    }

    @Test
    public void testGetValueOfMultipleDateTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Calendar[] value = callContentBeanMethod(generatedBean, MULTIPLE_DATE_TYPE_METHOD_NAME, Calendar[].class) ;

        assertNotNull("The method '" + MULTIPLE_DATE_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertThat(value.length, equalTo(2));
    }

    @Test
    public void testGetValueOfSingleValueMultipleDateTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Calendar[] value = callContentBeanMethod(generatedBean, SINGLE_VALUE_MULTIPLE_DATE_TYPE_METHOD_NAME, Calendar[].class);

        assertNotNull("The method '" + SINGLE_VALUE_MULTIPLE_DATE_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertThat(value.length, equalTo(1));
    }

    @Test
    public void testGetValueOfMultipleValueSingleDateTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Calendar value = callContentBeanMethod(generatedBean, MULTIPLE_VALUE_SINGLE_DATE_TYPE_METHOD_NAME, Calendar.class);

        assertNotNull("The method '" + MULTIPLE_VALUE_SINGLE_DATE_TYPE_METHOD_NAME + "' didn't return any value", value);

        Date result = dateParser.parse("25/03/2019");
        assertTrue(DateUtils.isSameDay(result, value.getTime()));
    }

    @Ignore // This test belongs to the improvement of CMS-11933
    @Test
    public void testGetValueOfCalendarDateTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        Calendar value = callContentBeanMethod(generatedBean, CALENDAR_DATE_TYPE_METHOD_NAME, Calendar.class);

        assertNotNull("The method '" + CALENDAR_DATE_TYPE_METHOD_NAME + "' didn't return any value", value);

        Date result = dateParser.parse("25/03/2019");
        assertTrue(DateUtils.isSameDay(result, value.getTime()));
    }

    @Ignore // This test belongs to the improvement of CMS-11933
    @Test
    public void testGetValueOfHtmlTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        String value = callContentBeanMethod(generatedBean, HTML_TYPE_METHOD_NAME, String.class);

        assertNotNull("The method '" + HTML_TYPE_METHOD_NAME + "' didn't return any value", value);
        assertEquals("htmltypecontent", value);
    }

    @Test
    public void testGetContentOfRichTextEditorCompoundTypeWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        HippoHtml hippoHtml = callContentBeanMethod(generatedBean, RICH_TEXT_EDITOR_COMPOUND_TYPE_METHOD_NAME, HippoHtml.class);

        assertNotNull("The method '" + RICH_TEXT_EDITOR_COMPOUND_TYPE_METHOD_NAME + "' didn't return any value", hippoHtml);

        assertEquals("richtexteditorcontent", hippoHtml.getContent());
    }

    @Ignore // This test belongs to the improvement of CMS-11933
    @Test
    public void testGetContentOfDocbaseTypeFieldWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        HippoBean hippoBean = callContentBeanMethod(generatedBean, DOCBASE_TYPE_METHOD_NAME, HippoBean.class);

        assertNotNull("The method '" + DOCBASE_TYPE_METHOD_NAME + "' didn't return any value", hippoBean);

        assertEquals("2dcef400-50e2-456e-9722-fd496defa56b", hippoBean.getNode().getIdentifier());
    }

    @Test
    public void testGetContentOfLinkCompoundTypeWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        HippoBean hippoBean = callContentBeanMethod(generatedBean, LINK_COMPOUND_TYPE_METHOD_NAME, HippoBean.class);

        assertNotNull("The method '" + LINK_COMPOUND_TYPE_METHOD_NAME + "' didn't return any value", hippoBean);

        assertEquals("64ab4648-0c20-40d2-9f18-d7a394f0334b", hippoBean.getNode().getParent().getIdentifier());
    }

    @Test
    public void testGetContentOfResourceCompoundTypeWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        HippoResourceBean hippoResourceBean = callContentBeanMethod(generatedBean, RESOURCE_COMPOUND_TYPE_METHOD_NAME, HippoResourceBean.class);

        assertNotNull("The method '" + RESOURCE_COMPOUND_TYPE_METHOD_NAME + "' didn't return any value", hippoResourceBean);

        assertEquals("picture_thumbnail.jpeg", hippoResourceBean.getFilename());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetContentOfContentBlocksTypeWithoutContentBean_withoutValidator() throws Exception {

        Object generatedBean = getContentBean();

        List<HippoBean> htmlBlocks = callContentBeanMethod(generatedBean, CONTENT_BLOCKS_TYPE_METHOD_NAME, List.class);

        assertNotNull("The method '" + CONTENT_BLOCKS_TYPE_METHOD_NAME + "' didn't return any value", htmlBlocks);

        HippoBean contentBlocksBean = htmlBlocks.get(0);
        HippoHtml contentBlocksText = (HippoHtml) contentBlocksBean.getClass().getMethod("getText").invoke(contentBlocksBean);

        assertEquals("Welcome Home!", contentBlocksText.getContent());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetContentOfContentBlocksTypeWithoutContentBean_withValidator() throws Exception {

        Object generatedBean = getContentBean();

        List<HippoBean> htmlBlocks = callContentBeanMethod(generatedBean, CONTENT_BLOCKS_TYPE_WITH_VALIDATOR_METHOD_NAME, List.class);

        assertNotNull("The method '" + CONTENT_BLOCKS_TYPE_WITH_VALIDATOR_METHOD_NAME + "' didn't return any value", htmlBlocks);

        HippoBean contentBlocksBean = htmlBlocks.get(0);
        HippoHtml contentBlocksText = (HippoHtml) contentBlocksBean.getClass().getMethod("getText").invoke(contentBlocksBean);

        assertEquals("Welcome Home with validator!", contentBlocksText.getContent());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetContentOfEssentialsGeneratedContentBlocksTypeWithoutContentBean() throws Exception {

        Object generatedBean = getContentBean();

        List<HippoBean> htmlBlocks = callContentBeanMethod(generatedBean, ESSENTIALS_GENERATED_CONTENT_BLOCKS_TYPE_METHOD_NAME, List.class);

        assertNotNull("The method '" + ESSENTIALS_GENERATED_CONTENT_BLOCKS_TYPE_METHOD_NAME + "' didn't return any value", htmlBlocks);

        HippoBean contentBlocksBean = htmlBlocks.get(0);
        HippoHtml contentBlocksText = (HippoHtml) contentBlocksBean.getClass().getMethod("getText").invoke(contentBlocksBean);

        assertEquals("Welcome Home with Essentials!", contentBlocksText.getContent());
    }

}
