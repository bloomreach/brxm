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
package org.onehippo.cms7.services.processor.html;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.jcr.RepositoryException;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.Serializer;
import org.htmlcleaner.TagNode;
import org.onehippo.cms7.services.processor.html.filter.HtmlFilter;
import org.onehippo.cms7.services.processor.html.filter.WhitelistHtmlFilter;
import org.onehippo.cms7.services.processor.html.serialize.HtmlSerializerFactory;
import org.onehippo.cms7.services.processor.html.util.StringUtil;
import org.onehippo.cms7.services.processor.html.visit.Tag;
import org.onehippo.cms7.services.processor.html.visit.TagVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlProcessorImpl implements HtmlProcessor {

    public static final Logger log = LoggerFactory.getLogger(HtmlProcessorImpl.class);

    private final HtmlProcessorConfig config;
    private final HtmlCleaner parser;
    private final HtmlFilter filter;
    private final Serializer serializer;

    public HtmlProcessorImpl(final HtmlProcessorConfig config) {
        this.config = config;

        final CleanerProperties properties = new CleanerProperties();
        properties.setOmitHtmlEnvelope(true);
        properties.setOmitXmlDeclaration(true);
        properties.setOmitComments(config.isOmitComments());

        parser = new HtmlCleaner(properties);
        filter = new WhitelistHtmlFilter(config.getWhitelistElements());
        serializer = HtmlSerializerFactory.create(config.getSerializer(), properties);
    }

    @Override
    public String read(final String html, final List<TagVisitor> visitors) throws IOException {
        final TagNode node = parse(html);
        visit(node, visitors, TagVisitor::onRead);

        String serialized = serialize(node);
        if (config.isConvertLineEndings()) {
            serialized = StringUtil.convertLfToCrlf(serialized);
        }

        return serialized;
    }

    @Override
    public String write(final String html, final List<TagVisitor> visitors) throws IOException {
        TagNode node = parse(html);

        visit(node, visitors, TagVisitor::onWrite);

        if (config.isFilter() && filter != null) {
            node = filter.apply(node);
        }

        String serialized = serialize(node);
        if (config.isConvertLineEndings()) {
            serialized = StringUtil.convertCrlfToLf(serialized);
        }

        return serialized;
    }

    private void visit(final TagNode node, final List<TagVisitor> visitors, final Visit visit) {
        if (visitors != null && !visitors.isEmpty()) {
            node.traverse((parentNode, htmlNode) -> {
                final Tag parent = Tag.from(parentNode);
                final Tag tag = Tag.from(htmlNode);
                visitors.forEach(visitor -> {
                    try {
                        visit.apply(visitor, parent, tag);
                    } catch (final RepositoryException e) {
                        log.info(e.getMessage(), e);
                    }
                });
                return true;
            });
        }
    }

    private TagNode parse(String html) {
        if (html == null) {
            html = "";
        }
        return parser.clean(html);
    }

    private String serialize(final TagNode html) throws IOException {
        if (html == null) {
            return "";
        }
        final StringWriter writer = new StringWriter();
        serializer.write(html, writer, config.getCharset());
        return writer.getBuffer().toString().trim();
    }

    private interface Visit {
        void apply(final TagVisitor visitor, final Tag parent, final Tag tag) throws RepositoryException;
    }
}
