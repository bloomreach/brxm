/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.sf.json.JSONObject;

class RepositoryResourceBundleLoader extends ResourceBundleLoader {

    private static final Logger log = LoggerFactory.getLogger(RepositoryResourceBundleLoader.class);

    RepositoryResourceBundleLoader(final Collection<String> locales) {
        super(locales);
    }

    @Override
    protected void collectResourceBundles(final ArtifactInfo artifactInfo, final Collection<ResourceBundle> bundles) throws IOException {
        try {
            // for all jars with hippoecm-extension.xml files ...
            final ZipFile zipFile = new ZipFile(artifactInfo.getJarFile());
            final ZipEntry extension = zipFile.getEntry("hippoecm-extension.xml");
            if (extension != null) {
                // ... gather all the hippo:resourcebundle initialize items ...
                ExtensionParser extensionParser = new ExtensionParser();
                try (InputStream extensionStream = zipFile.getInputStream(extension)) {
                    extensionParser.parse(extensionStream);
                }
                for (String resourceBundlesFile : extensionParser.resourceBundles) {
                    final ZipEntry bundleEntry = zipFile.getEntry(resourceBundlesFile);
                    if (bundleEntry != null) {
                        // ... such hippo:resourcebundle files contain multiple resource bundles ... collect them
                        try (InputStream inputStream = zipFile.getInputStream(bundleEntry)) {
                            final String json = IOUtils.toString(inputStream);
                            final JSONObject jsonObject = JSONObject.fromObject(json);
                            // ... the resource bundle representations need to contain enough information
                            // to assemble a new hippo:resourcebundle file for a different language later
                            // ... this information is encoded in the key
                            collectResourceBundles(jsonObject, resourceBundlesFile, locales, new Path(), bundles);
                        }
                    } else {
                        log.warn("Extension file contains invalid resourcebundle: '{}' not found in jar", resourceBundlesFile);
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    static void collectResourceBundles(final JSONObject jsonObject, final String fileName, final Collection<String> locales, final Path path, final Collection<ResourceBundle> bundles) {
        final Map<String, String> entries = new HashMap<>();
        for (Object key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                path.push(key.toString());
                collectResourceBundles((JSONObject) value, fileName, locales, path, bundles);
                path.pop();
            } else if (value instanceof String) {
                entries.put(key.toString(), value.toString());
            }
        }
        if (!entries.isEmpty() && locales.contains(path.toLocale())) {
            bundles.add(new RepositoryResourceBundle(path.toBundleName(), fileName, path.toLocale(), entries));
        }
    }
    
    /**
     * Collects all hippo:resourcebundle entries from a hippoecm-extension.xml file
     */
    static class ExtensionParser extends DefaultHandler {

        private final Collection<String> resourceBundles = new ArrayList<>();
        private boolean resourceBundlesProperty = false;
        private boolean resourceBundlesValueProperty = false;

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
            if (qName.equals("sv:property") && attributes.getValue("sv:name").equals("hippo:resourcebundles")) {
                resourceBundlesProperty = true;
            } else if (resourceBundlesProperty && qName.equals("sv:value")) {
                resourceBundlesValueProperty = true;
            }
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            if (resourceBundlesValueProperty) {
                resourceBundles.add(String.valueOf(ch, start, length));
            }
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            if (resourceBundlesProperty && qName.equals("sv:property")) {
                resourceBundlesProperty = false;
            } else if (resourceBundlesValueProperty && qName.equals("sv:value")) {
                resourceBundlesValueProperty = false;
            }
        }

        Collection<String> parse(final InputStream in) throws IOException {
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
                SAXParser parser = factory.newSAXParser();
                parser.parse(new InputSource(in), this);
            } catch (FactoryConfigurationError e) {
                throw new IOException("SAX parser implementation not available", e);
            } catch (ParserConfigurationException e) {
                throw new IOException("SAX parser configuration error", e);
            } catch (SAXException e) {
                throw new IOException("SAX parsing error", e);
            }
            return resourceBundles;
        }

    }

    /**
     * Utility for keeping track of the path inside a repository resource bundle import file.
     */
    static class Path {

        private final Stack<String> elements = new Stack<>();

        Path() {
        }

        private void push(String element) {
            elements.push(element);
        }

        private void pop() {
            elements.pop();
        }

        private String peek() {
            return elements.peek();
        }
        
        String toBundleName() {
            final StringBuilder sb = new StringBuilder();
            final Iterator<String> iterator = elements.iterator();
            while (iterator.hasNext()) {
                final String element = iterator.next();
                if (iterator.hasNext()) {
                    if (sb.length() > 0) {
                        sb.append('.');
                    }
                    sb.append(element);
                }
            }
            return sb.toString();
        }
        
        String toLocale() {
            return peek();
        }
    }

}
