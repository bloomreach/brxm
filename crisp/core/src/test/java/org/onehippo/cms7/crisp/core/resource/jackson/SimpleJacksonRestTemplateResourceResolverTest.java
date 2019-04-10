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
package org.onehippo.cms7.crisp.core.resource.jackson;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.core.resource.SpringResourceBinary;
import org.springframework.security.util.InMemoryResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SimpleJacksonRestTemplateResourceResolverTest {

    private static final String HELLO = "Hello, world!";
    private static final String HELLO_TXT = "File: hello.txt\n" + HELLO;
    private static final String HELLO_JSON = "{ \"message\": \"" + HELLO + "\" }";

    private Map<String, Binary> binariesMap;
    private SimpleJacksonRestTemplateResourceResolver resolver;

    @Before
    public void setUp() throws Exception {
        binariesMap = new HashMap<>();
        binariesMap.put("/hello.txt", new SpringResourceBinary(new InMemoryResource(HELLO_TXT.getBytes("UTF-8")), true));
        binariesMap.put("/hello.json", new SpringResourceBinary(new InMemoryResource(HELLO_JSON.getBytes("UTF-8")), true));

        resolver = new SimpleJacksonRestTemplateResourceResolver() {
            @Override
            public Binary resolveBinary(String absPath, Map<String, Object> pathVariables,
                    ExchangeHint exchangeHint) throws ResourceException {
                return binariesMap.get(absPath);
            }
        };
    }

    @Test
    public void testResolveBinary() throws Exception {
        Binary binary;

        binary = resolver.resolveBinary("/hello.txt");
        assertNotNull(binary);
        assertEquals(HELLO_TXT, IOUtils.toString(binary.getInputStream(), "UTF-8"));

        binary = resolver.resolveBinary("/hello.json");
        assertNotNull(binary);
        assertEquals(HELLO_JSON, IOUtils.toString(binary.getInputStream(), "UTF-8"));
    }

    @Test
    public void testResolveBinaryAsResource() throws Exception {
        Resource resource;

        try {
            resource = resolver.resolveBinaryAsResource("/hello.txt");
            fail("Should not be able to convert the text data into JacksonResource.");
        } catch (ResourceException expected) {
        }

        resource = resolver.resolveBinaryAsResource("/hello.json");
        assertNotNull(resource);
        assertEquals(HELLO, resource.getValue("message"));
    }

}
