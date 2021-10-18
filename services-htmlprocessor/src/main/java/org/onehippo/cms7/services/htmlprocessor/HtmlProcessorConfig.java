/*
 *  Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.htmlprocessor.filter.Element;
import org.onehippo.cms7.services.htmlprocessor.serialize.HtmlSerializer;

public class HtmlProcessorConfig implements Serializable {

    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final boolean DEFAULT_FILTER = false;
    private static final boolean DEFAULT_OMIT_COMMENTS = false;
    private static final boolean DEFAULT_OMIT_DATA_PROTOCOL = true;
    private static final boolean DEFAULT_OMIT_JS_PROTOCOL = true;
    private static final boolean DEFAULT_CONVERT_LINE_ENDINGS = true;
    private static final boolean DEFAULT_SECURE_TARGET_BLANK_LINKS = true;
    private static final HtmlSerializer DEFAULT_SERIALIZER = HtmlSerializer.SIMPLE;

    // repository property names
    private static final String CHARSET = "charset";
    private static final String OMIT_COMMENTS = "omitComments";
    private static final String OMIT_DATA_PROTOCOL = "omitDataProtocol";
    private static final String OMIT_JS_PROTOCOL = "omitJavascriptProtocol";
    private static final String SECURE_TARGET_BLANK_LINKS = "secureTargetBlankLinks";
    private static final String CONVERT_LINE_ENDINGS = "convertLineEndings";
    private static final String SERIALIZER = "serializer";
    private static final String FILTER = "filter";
    private static final String ATTRIBUTES = "attributes";

    private String charset;
    private HtmlSerializer serializer;
    private boolean omitComments;
    private boolean omitDataProtocol;
    private boolean omitJsProtocol;
    private boolean secureTargetBlankLinks;
    private boolean filter;
    private boolean convertLineEndings;
    private List<Element> whitelistElements;

    public HtmlProcessorConfig() {
        charset = DEFAULT_CHARSET;
        filter = DEFAULT_FILTER;
        convertLineEndings = DEFAULT_CONVERT_LINE_ENDINGS;
        serializer = DEFAULT_SERIALIZER;
        omitComments = DEFAULT_OMIT_COMMENTS;
        omitDataProtocol = DEFAULT_OMIT_DATA_PROTOCOL;
        omitJsProtocol = DEFAULT_OMIT_JS_PROTOCOL;
        secureTargetBlankLinks = DEFAULT_SECURE_TARGET_BLANK_LINKS;
    }

    public void reconfigure(final Node node) throws RepositoryException {
        charset = JcrUtils.getStringProperty(node, CHARSET, DEFAULT_CHARSET);
        convertLineEndings = JcrUtils.getBooleanProperty(node, CONVERT_LINE_ENDINGS, DEFAULT_CONVERT_LINE_ENDINGS);
        filter = JcrUtils.getBooleanProperty(node, FILTER, DEFAULT_FILTER);
        omitComments = JcrUtils.getBooleanProperty(node, OMIT_COMMENTS, DEFAULT_OMIT_COMMENTS);
        omitDataProtocol = JcrUtils.getBooleanProperty(node, OMIT_DATA_PROTOCOL, DEFAULT_OMIT_DATA_PROTOCOL);
        omitJsProtocol = JcrUtils.getBooleanProperty(node, OMIT_JS_PROTOCOL, DEFAULT_OMIT_JS_PROTOCOL);
        secureTargetBlankLinks = JcrUtils.getBooleanProperty(node, SECURE_TARGET_BLANK_LINKS, DEFAULT_SECURE_TARGET_BLANK_LINKS);

        final String serializerName = JcrUtils.getStringProperty(node, SERIALIZER, DEFAULT_SERIALIZER.name());
        serializer = HtmlSerializer.valueOfOrDefault(serializerName);

        if (node.hasNodes()) {
            whitelistElements = new ArrayList<>();
            final NodeIterator filters = node.getNodes();
            while (filters.hasNext()) {
                final Element element = createElement(filters.nextNode());
                whitelistElements.add(element);
            }
        }
    }

    private Element createElement(final Node node) throws RepositoryException {
        final boolean omitJsForElement = JcrUtils.getBooleanProperty(node, OMIT_JS_PROTOCOL, omitJsProtocol);
        final boolean omitDataForElement = JcrUtils.getBooleanProperty(node, OMIT_DATA_PROTOCOL, omitDataProtocol);
        final boolean secureTargetBlankLinksForElement = JcrUtils.getBooleanProperty(node, SECURE_TARGET_BLANK_LINKS, secureTargetBlankLinks);

        final String[] attributes = JcrUtils.getMultipleStringProperty(node, ATTRIBUTES, new String[]{});
        final String configName = node.getName();
        final int offset = configName.lastIndexOf('.');
        final String elementName = offset != -1 ? configName.substring(offset + 1) : configName;

        return Element.create(elementName, attributes)
                .setOmitJsProtocol(omitJsForElement)
                .setOmitDataProtocol(omitDataForElement)
                .setSecureTargetBlankLinks(secureTargetBlankLinksForElement);
    }

    public void setSerializer(final HtmlSerializer serializer) {
        this.serializer = serializer;
    }

    public HtmlSerializer getSerializer() {
        return serializer;
    }

    public void setCharset(final String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return charset;
    }

    public void setFilter(final boolean filter) {
        this.filter = filter;
    }

    public boolean isFilter() {
        return filter;
    }

    public void setWhitelistElements(final List<Element> whitelistElements) {
        this.whitelistElements = whitelistElements;
    }

    public List<Element> getWhitelistElements() {
        return whitelistElements;
    }

    public boolean isConvertLineEndings() {
        return convertLineEndings;
    }

    public void setConvertLineEndings(final boolean convertLineEndings) {
        this.convertLineEndings = convertLineEndings;
    }

    public void setOmitComments(final boolean omitComments) {
        this.omitComments = omitComments;
    }

    public boolean isOmitComments() {
        return omitComments;
    }

    public void setOmitDataProtocol(final boolean omitDataProtocol) {
        this.omitDataProtocol = omitDataProtocol;
    }

    public boolean isOmitDataProtocol() {
        return omitDataProtocol;
    }

    public void setOmitJavascriptProtocol(final boolean omitJsProtocol) {
        this.omitJsProtocol = omitJsProtocol;
    }

    public boolean isOmitJavascriptProtocol() {
        return omitJsProtocol;
    }

    public boolean isSecureTargetBlankLinks() {
        return secureTargetBlankLinks;
    }
}
