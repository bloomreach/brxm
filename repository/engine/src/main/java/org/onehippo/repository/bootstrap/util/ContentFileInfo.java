/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_NAME;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_NODE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTROOT;
import static org.onehippo.repository.bootstrap.util.BootstrapUtils.getResource;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.COMBINE;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.ESV_URI;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.MERGE;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.OVERLAY;

public class ContentFileInfo {

    public final List<String> contextPaths;
    public final String deltaDirective;

    private ContentFileInfo(final List<String> contextPaths, final String deltaDirective) {
        this.contextPaths = contextPaths;
        this.deltaDirective = deltaDirective;
    }

    public boolean isMerge() {
        return "combine".equals(deltaDirective) || "overlay".equals(deltaDirective);
    }

    public static ContentFileInfo readInfo(final Node item) {
        ContentFileInfoReader contentFileInfoReader = null;
        InputStream in = null;
        String contentResource = null;
        try {
            contentResource = StringUtils.trim(item.getProperty(HIPPO_CONTENTRESOURCE).getString());
            if (contentResource.endsWith(".zip") || contentResource.endsWith(".jar")) {
                return null;
            }
            final String contentRoot = StringUtils.trim(item.getProperty(HIPPO_CONTENTROOT).getString());
            contentFileInfoReader = new ContentFileInfoReader(contentRoot);
            final URL contentResourceURL = getResource(item, contentResource);
            if (contentResourceURL != null) {
                in = contentResourceURL.openStream();
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
                factory.newSAXParser().parse(new InputSource(in), contentFileInfoReader);
            }
        } catch (ContentFileInfoReadingShortCircuitException ignore) {
        } catch (FactoryConfigurationError | SAXException | ParserConfigurationException | IOException | RepositoryException e) {
            BootstrapConstants.log.error("Could not read root node name from content file {}", contentResource, e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return contentFileInfoReader == null ? null : contentFileInfoReader.getContentFileInfo();
    }

    private static class ContentFileInfoReader extends DefaultHandler {

        private final Stack<String> path = new Stack<>();
        private final List<String> contextPaths = new ArrayList<>();
        private final String contentRoot;
        private String deltaDirective;
        private int depth = -1;

        private ContentFileInfoReader(final String contentRoot) {
            this.contentRoot = contentRoot.equals("/") ? "" : contentRoot;
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName, final org.xml.sax.Attributes atts) throws SAXException {
            final Name name = NameFactoryImpl.getInstance().create(uri, localName);
            if (name.equals(SV_NODE)) {
                if (skip()) {
                    depth++;
                    return;
                }
                final String svName = atts.getValue(SV_NAME.getNamespaceURI(), SV_NAME.getLocalName());
                final String esvMerge = atts.getValue(ESV_URI, MERGE);
                if (contextPaths.isEmpty()) {
                    path.push(svName);
                    contextPaths.add(getCurrenContextPath());
                    deltaDirective = esvMerge;
                    if (!isMergeCombine(esvMerge)) {
                        throw new ContentFileInfoReadingShortCircuitException();
                    }
                } else {
                    if (isMergeCombine(esvMerge)) {
                        path.push(svName);
                        contextPaths.add(getCurrenContextPath());
                    } else {
                        depth++;
                    }
                }
            }
        }


        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            final Name name = NameFactoryImpl.getInstance().create(uri, localName);
            if (name.equals(SV_NODE)) {
                if (skip()) {
                    depth--;
                } else {
                    path.pop();
                }
            }
        }

        private boolean isMergeCombine(final String esvMerge) {
            return COMBINE.equals(esvMerge) || OVERLAY.equals(esvMerge);
        }

        private ContentFileInfo getContentFileInfo() {
            if (!contextPaths.isEmpty()) {
                return new ContentFileInfo(contextPaths, deltaDirective);
            }
            return null;
        }

        private String getCurrenContextPath() {
            StringBuilder sb = new StringBuilder(contentRoot);
            for (String pathElement : path) {
                sb.append("/").append(pathElement);
            }
            return sb.toString();
        }

        private boolean skip() {
            return depth > -1;
        }
    }

    private static class ContentFileInfoReadingShortCircuitException extends SAXException {}

}
