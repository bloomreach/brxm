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
package org.hippoecm.hst.pagemodelapi.common.content.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.hippoecm.hst.content.annotations.PageModelIgnore;
import org.hippoecm.hst.content.annotations.PageModelIgnoreType;
import org.hippoecm.hst.content.annotations.PageModelProperty;
import org.hippoecm.hst.content.beans.ContentTypesProvider;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.VersionedObjectConverterProxy;
import org.hippoecm.hst.content.beans.dynamic.DynamicBeanInterceptor;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HippoBeansApiObjectMapperIT extends RepositoryTestCase {

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
        RepositoryTestCase.setUpClass();
    }

    @Node(jcrType="unittestproject:textpage")
    public static class TextBean extends HippoDocument {
        public String getTitle() {
            return getSingleProperty("unittestproject:title");
        }

        public HippoHtml getBody(){
            return getHippoHtml("unittestproject:body");
        }
    }

    public static class TextBeanExtra extends TextBean {

        public String getTest() {
            return "test-yes";
        }

        @PageModelProperty("testPropName")
        public String getPropMethodName() {
            return "test-yes";
        }

        @PageModelIgnore
        public String getIgnoreProperty() {
            return "test-yes";
        }

        public TestIgnore getTestIgnore() {
            return new TestIgnore();
        }
    }

    @PageModelIgnoreType
    public static class TestIgnore {

        private final String testIgnore = "test-yes";

        public String getTestIgnore() {
            return testIgnore;
        }
    }

    protected Collection<Class<? extends HippoBean>> getAnnotatedClasses() {
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
        annotatedClasses.add(TextBeanExtra.class);
        return annotatedClasses;
    }

    protected ObjectConverter createObjectConverter() {
        return createObjectConverter((List<Class<? extends HippoBean>>) getAnnotatedClasses());
    }

    protected ObjectConverter createObjectConverter(List<Class<? extends HippoBean>> annotatedClasses) {
        return createObjectConverter(annotatedClasses, Collections.emptyList());

    }

    protected ObjectConverter createObjectConverter(List<Class<? extends HippoBean>> annotatedNodeClasses,
            List<Class<? extends DynamicBeanInterceptor>> annotatedInterceptorClasses) {
        return new VersionedObjectConverterProxy(annotatedNodeClasses, annotatedInterceptorClasses,
                new ContentTypesProvider(HippoServiceRegistry.getService(ContentTypeService.class)), true);
    }


    @Test
    public void test_document_json_serialization() throws Exception {
        ObjectConverter objectConverter = createObjectConverter();
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);
        TextBeanExtra homeBean = (TextBeanExtra)obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");

        ObjectMapper beansObjectMapper = new PageModelObjectMapperFactory().createPageModelObjectMapper();

        String serialized = beansObjectMapper.writeValueAsString(homeBean);

        assertTrue(serialized.contains("\"test\":\"test-yes\""));
        assertTrue(serialized.contains("\"testPropName\":\"test-yes\""));
        assertFalse(serialized.contains("\"testIgnore\""));
        assertFalse(serialized.contains("\"ignoreProperty\""));


        assertTrue(serialized.contains("\"localeString\":\"en_US\""));

        assertFalse("don't include versioned node info since irrelevant", serialized.contains("\"versionedNode\":\"false\""));
    }


}
