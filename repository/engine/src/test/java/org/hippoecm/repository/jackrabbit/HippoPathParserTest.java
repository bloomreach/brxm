/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.repository.jackrabbit;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class HippoPathParserTest {

    @Test
    public void testParsing() throws RepositoryException {
        final PathFactory pathFactory = PathFactoryImpl.getInstance();
        final NamespaceResolver namespaceResolver = QDefinitionBuilderFactory.NS_DEFAULTS;
        final HippoNamePathResolver resolver = new HippoNamePathResolver(namespaceResolver, false);
        Path path = HippoPathParser.parse("/binaries/foldername/documentname/versions[2]/file", resolver, pathFactory);
        assertNotNull(path);
        path = HippoPathParser.parse("/binaries/foldername/documentname/versions['2']/file", resolver, pathFactory);
        assertNotNull(path);
        path = HippoPathParser.parse("/binaries/foldername/documentname/versions'/file", resolver, pathFactory);
        assertNotNull(path);
        path = HippoPathParser.parse("/binaries/foldername/documentname/versions'xx'/file", resolver, pathFactory);
        assertNotNull(path);

    }

    @Test(expected = MalformedPathException.class)
    public void testSingleQuoteParsingPrefix() throws RepositoryException {
        final PathFactory pathFactory = PathFactoryImpl.getInstance();
        final NamespaceResolver namespaceResolver = QDefinitionBuilderFactory.NS_DEFAULTS;
        final HippoNamePathResolver resolver = new HippoNamePathResolver(namespaceResolver, false);
        HippoPathParser.parse("/binaries/foldername/documentname/versions['2]/file", resolver, pathFactory);
    }

    @Test(expected = MalformedPathException.class)
    public void testSingleQuoteParsingSuffix() throws RepositoryException {
        final PathFactory pathFactory = PathFactoryImpl.getInstance();
        final NamespaceResolver namespaceResolver = QDefinitionBuilderFactory.NS_DEFAULTS;
        final HippoNamePathResolver resolver = new HippoNamePathResolver(namespaceResolver, false);
        HippoPathParser.parse("/binaries/foldername/documentname/versions[2']/file", resolver, pathFactory);
    }

}