/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
    private static final boolean DEFAULT_OMIT_JAVASCRIPT_PROTOCOL = true;
    private static final boolean DEFAULT_CONVERT_LINE_ENDINGS = true;
    private static final HtmlSerializer DEFAULT_SERIALIZER = HtmlSerializer.SIMPLE;

    // repository property names
    private static final String CHARSET = "charset";
    private static final String OMIT_COMMENTS = "omitComments";
    private static final String OMIT_JAVASCRIPT_PROTOCOL = "omitJavascriptProtocol";
    private static final String CONVERT_LINE_ENDINGS = "convertLineEndings";
    private static final String SERIALIZER = "serializer";
    private static final String FILTER = "filter";
    private static final String ATTRIBUTES = "attributes";

    private String charset;
    private HtmlSerializer serializer;
    private boolean omitComments;
    private boolean omitJavascriptProtocol;
    private boolean filter;
    private boolean convertLineEndings;
    private List<Element> whitelistElements;

    public HtmlProcessorConfig() {
        charset = DEFAULT_CHARSET;
        filter = DEFAULT_FILTER;
        convertLineEndings = DEFAULT_CONVERT_LINE_ENDINGS;
        serializer = DEFAULT_SERIALIZER;
        omitComments = DEFAULT_OMIT_COMMENTS;
        omitJavascriptProtocol = DEFAULT_OMIT_JAVASCRIPT_PROTOCOL;
    }

    public void reconfigure(final Node node) throws RepositoryException {
        charset = JcrUtils.getStringProperty(node, CHARSET, DEFAULT_CHARSET);
        convertLineEndings = JcrUtils.getBooleanProperty(node, CONVERT_LINE_ENDINGS, DEFAULT_CONVERT_LINE_ENDINGS);
        filter = JcrUtils.getBooleanProperty(node, FILTER, DEFAULT_FILTER);
        omitComments = JcrUtils.getBooleanProperty(node, OMIT_COMMENTS, DEFAULT_OMIT_COMMENTS);
        omitJavascriptProtocol = JcrUtils.getBooleanProperty(node, OMIT_JAVASCRIPT_PROTOCOL, DEFAULT_OMIT_JAVASCRIPT_PROTOCOL);

        final String serializerName = JcrUtils.getStringProperty(node, SERIALIZER, DEFAULT_SERIALIZER.name());
        serializer = HtmlSerializer.valueOfOrDefault(serializerName);

        if (node.hasNodes()) {
            final String[] emptyAttr = new String[]{};
            whitelistElements = new ArrayList<>();
            final NodeIterator filters = node.getNodes();
            while (filters.hasNext()) {
                final Node filterNode = filters.nextNode();
                final String[] attributes = JcrUtils.getMultipleStringProperty(filterNode, ATTRIBUTES, emptyAttr);
                final String configName = filterNode.getName();
                final int offset = configName.lastIndexOf('.');
                final String elementName = offset != -1 ? configName.substring(offset + 1) : configName;
                final Element element = Element.create(elementName, attributes);
                whitelistElements.add(element);
            }
        }
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

    public void setOmitComments(final boolean omitComments) {
        this.omitComments = omitComments;
    }

    public boolean isOmitComments() {
        return omitComments;
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

    public boolean isOmitJavascriptProtocol() {
        return omitJavascriptProtocol;
    }

    public void setOmitJavascriptProtocol(final boolean omitJsProtocol) {
        this.omitJavascriptProtocol = omitJsProtocol;
    }

}
