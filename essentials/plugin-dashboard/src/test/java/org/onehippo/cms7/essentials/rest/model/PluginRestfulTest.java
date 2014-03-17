/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.model.VendorRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class PluginRestfulTest {

    private static Logger log = LoggerFactory.getLogger(PluginRestfulTest.class);

    @Test
    public void testJaxb() throws Exception {
        final PluginRestful value = new PluginRestful();
        value.setName("com.foo.name");
        final Calendar today = Calendar.getInstance();
        value.setDateInstalled(today);
        value.addRestCLass("com.foo.Foo");
        value.addRestCLass("com.foo.Bar");
        final VendorRestful vendor = new VendorRestful();
        vendor.setName("hippo");
        value.setVendor(vendor);
        final JAXBContext context = JAXBContext.newInstance(PluginRestful.class);
        final Marshaller m = context.createMarshaller();
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final StringWriter writer = new StringWriter();
        m.marshal(value, writer);
        log.info("{}", writer.toString());
        final PluginRestful fromXml = (PluginRestful) unmarshaller.unmarshal(new StringReader(writer.toString()));
        log.info("fromXml {}", fromXml);
        assertEquals(2, fromXml.getRestClasses().size());
        assertEquals(today.getTime(), fromXml.getDateInstalled().getTime());
        assertEquals(vendor.getName(), fromXml.getVendor().getName());


    }
}
