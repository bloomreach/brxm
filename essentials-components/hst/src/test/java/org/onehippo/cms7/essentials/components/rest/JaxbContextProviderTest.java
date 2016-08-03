/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.rest;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.onehippo.cms7.essentials.components.paging.DefaultPagination;
import org.onehippo.cms7.essentials.components.paging.TestBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class JaxbContextProviderTest {

    private static final Logger log = LoggerFactory.getLogger(JaxbContextProviderTest.class);
    @Test
    public void testGetClasses() throws Exception {
        final List<TestBean> testBeans = new ArrayList<>();
        final TestBean testBean = new TestBean();
        testBeans.add(testBean);
        final DefaultPagination<TestBean> value = new DefaultPagination<>(testBeans);
        final JaxbContextProvider provider = new JaxbContextProvider();
        provider.setBeansPackage(TestBean.class.getPackage().getName());
        final JAXBContext context = provider.getContext(Object.class);
        final Marshaller m = context.createMarshaller();
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final StringWriter writer = new StringWriter();
        m.marshal(value, writer);
        final String xml = writer.toString();
        log.info("writer {}", xml);
        @SuppressWarnings("unchecked")
        final DefaultPagination<TestBean> v = (DefaultPagination<TestBean>)unmarshaller.unmarshal(new StringReader(xml));
        assertTrue(v.getItems().size() == 1);
        assertTrue(v.getItems().get(0).getTitle().equals(testBean.getTitle()));

    }
}