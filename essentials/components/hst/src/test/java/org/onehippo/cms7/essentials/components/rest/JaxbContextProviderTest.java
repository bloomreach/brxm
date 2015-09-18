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