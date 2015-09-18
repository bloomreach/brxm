package org.onehippo.cms7.essentials.components.paging;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class IterablePaginationTest {

    private static final Logger log = LoggerFactory.getLogger(IterablePaginationTest.class);
    @Test
    public void testJaxb() throws Exception {
        final List<TestBean> testBeans = new ArrayList<>();
        final TestBean testBean = new TestBean();
        testBeans.add(testBean);
        final IterablePagination<TestBean> value = new IterablePagination<>(testBeans, 1, 1);
        final JAXBContext context = JAXBContext.newInstance(IterablePagination.class, TestBean.class);
        final Marshaller m = context.createMarshaller();
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final StringWriter writer = new StringWriter();
        m.marshal(value, writer);
        final String xml = writer.toString();
        log.info("writer {}", xml);
        @SuppressWarnings("unchecked")
        final IterablePagination<TestBean> v = (IterablePagination<TestBean>)unmarshaller.unmarshal(new StringReader(xml));
        assertTrue(v.getItems().size() == 1);
        assertTrue(v.getItems().get(0).getTitle().equals(testBean.getTitle()));


    }
}