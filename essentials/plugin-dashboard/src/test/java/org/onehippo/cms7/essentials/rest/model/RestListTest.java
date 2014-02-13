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

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class RestListTest {

    private static Logger log = LoggerFactory.getLogger(RestListTest.class);

    @Test
    public void testList() throws Exception {

        final Configuration config = new Configuration();
        config.setIgnoreNamespaces(true);


        /*
          <property name="ignoreNamespaces" value="true"/>
    <property name="dropRootElement" value="true"/>
    <property name="serializeAsArray" value="true"/>
    <!--<property name="dropCollectionWrapperElement" value="true"/>-->
    <!--<property name="supportUnwrapped" value="true"/>-->
    <property name="arrayKeys">
      <list>
        <value>items</value>
        <value>item</value>
        <value>properties</value>
        <value>plugins</value>
        <value>variants</value>
        <value>imageSets</value>
        <value>translations</value>
        <value>restClasses</value>
      </list>*/
        final JAXBContext jc = JAXBContext.newInstance(RestList.class);
        final Writer writer = new StringWriter();
        final XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(new MappedNamespaceConvention(config), writer);

        final Marshaller marshaller = jc.createMarshaller();
        final RestList<KeyValueRestful> list = new RestList<>();
        list.add(new KeyValueRestful("test", "test"));
        list.add(new KeyValueRestful("test1", "test1"));
        marshaller.marshal(list, xmlStreamWriter);
        log.info("{}", writer);
        final JSONObject obj = new JSONObject(writer.toString());
        final XMLStreamReader xmlStreamReader = new MappedXMLStreamReader(obj);
        final Unmarshaller unmarshaller = jc.createUnmarshaller();
        @SuppressWarnings("unchecked")
        final RestfulList<KeyValueRestful> myList = (RestfulList<KeyValueRestful>) unmarshaller.unmarshal(xmlStreamReader);
        assertEquals(2, myList.getItems().size());


    }
}
