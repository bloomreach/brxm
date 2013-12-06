/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.scxml;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.apache.commons.scxml2.PathResolver;
import org.junit.Test;

/**
 * RepositoryURLResolverTest
 */
public class RepositoryURLResolverTest {

    @Test
    public void testWithNullBaseURL() throws Exception {
        final String source = "http://www.example.com/scxml/hello";
        PathResolver resolver = new RepositorySCXMLRegistry.RepositoryURLResolver(null);
        URL url = new URL(resolver.resolvePath(source));
        assertEquals(source, url.toString());
    }

    @Test
    public void testWithBaseURL() throws Exception {
        final String base = "http://www.example.com";
        final String source = "/scxml/hello";
        PathResolver resolver = new RepositorySCXMLRegistry.RepositoryURLResolver(new URL(base));
        URL url = new URL(resolver.resolvePath(source));
        assertEquals(base + source, url.toString());

        final String absSource = "http://www.onehippo.org/scxml/world";
        url = new URL(resolver.resolvePath(absSource));
        assertEquals(absSource, url.toString());
    }
}
