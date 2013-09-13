/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.shared.model;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id: PluginTest.java 157469 2013-03-08 14:03:30Z mmilicevic $"
 */
public class PluginTest {

    private static Logger log = LoggerFactory.getLogger(PluginTest.class);

    @Test
    public void testPluginMarshalling() throws Exception {
        final Plugin value = new Plugin();
        final Version version = new Version("1.00.00");
        value.addVersion(version);
        value.addVersion(new Version("1.00.01"));
        value.addVersion(new Version("1.00.02"));
        final Vendor vendor = new Vendor();
        vendor.setName("Hippo");
        vendor.setUrl("http://www.onehippo.com");
        vendor.setLogo("http://www.onehippo.com/images/hippo-logo.png");
        value.setVendor(vendor);

        final JAXBContext context = JAXBContext.newInstance(Plugin.class);
        final Marshaller m = context.createMarshaller();
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final StringWriter writer = new StringWriter();
        m.marshal(value, writer);
        log.info("writer {}", writer);
        Plugin v = (Plugin) unmarshaller.unmarshal(new StringReader(writer.toString()));
        assertTrue(v !=null);

    }
}
