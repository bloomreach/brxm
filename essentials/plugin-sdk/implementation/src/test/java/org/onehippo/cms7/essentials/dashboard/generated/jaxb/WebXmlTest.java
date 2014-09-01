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

package org.onehippo.cms7.essentials.dashboard.generated.jaxb;


import java.io.InputStream;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WebXmlTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(WebXmlTest.class);

    public static final String EXISTING_VALUE = "classpath*:org/example/beans/**/*.class\n" +
            "      ,classpath*:org/onehippo/forge/**/*.class";
    public static final String ADDED_VALUE = "classpath*:org/onehippo/cms7/hst/beans/**/*.class," +
            "    classpath*:org/onehippo/forge/robotstxt/**/*.class";

    public static final String EXPECTED_RESULT = EXISTING_VALUE + ',' + ADDED_VALUE;

    @Test
    public void testParsing() throws Exception {


        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("test_web.xml")) {


            final JAXBContext context = JAXBContext.newInstance(WebXml.class);
            final Marshaller m = context.createMarshaller();
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            final WebXml webXml = (WebXml) unmarshaller.unmarshal(stream);
            log.info("webXml {}", webXml);
            assertEquals(2, webXml.getParameters().size());
            final String hstBeanContextValue = webXml.getHstBeanContextValue();
            assertNotNull(hstBeanContextValue);
            // read stream again and do string replacement:

            @SuppressWarnings("resource")
            final InputStream webXmlStream = getClass().getClassLoader().getResourceAsStream("test_web.xml");
            final String newWebContent = webXml.addToHstBeanContextValue(webXmlStream, ADDED_VALUE);
            log.info("newWebContent {}", newWebContent);
            final WebXml newWebXml = (WebXml) unmarshaller.unmarshal(new StringReader(newWebContent));
            assertEquals(EXPECTED_RESULT, newWebXml.getHstBeanContextValue());

        }


    }
}