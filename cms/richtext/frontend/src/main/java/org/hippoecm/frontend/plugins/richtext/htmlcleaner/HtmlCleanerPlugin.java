/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.htmlcleaner;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.IHtmlCleanerService;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CompactHtmlSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyHtmlSerializer;
import org.htmlcleaner.Serializer;
import org.htmlcleaner.SimpleHtmlSerializer;
import org.htmlcleaner.TagNode;


public class HtmlCleanerPlugin extends Plugin implements IHtmlCleanerService {

    private static final long serialVersionUID = 1L;

    private static final String WHITELIST = "whitelist";
    private static final String ATTRIBUTES = "attributes";
    private static final String CHARSET = "charset";
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String SERIALIZER = "serializer";
    private static final String COMPACT = "compact";
    private static final String PRETTY = "pretty";
    private static final String OMIT_COMMENTS = "omitComments";
    private static final String JAVASCRIPT_PROTOCOL = "javascript:";
    private static final TextEscaper escaper = new TextEscaper();

    private final Map<String, Element> whitelist = new HashMap<>();
    private final String charset;
    private final String serializer;
    private final boolean omitComments;


    public HtmlCleanerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        charset = config.getString(CHARSET, DEFAULT_CHARSET);
        serializer = config.getString(SERIALIZER, COMPACT);
        omitComments = config.getBoolean(OMIT_COMMENTS);
        final IPluginConfig whitelistConfig = config.getPluginConfig(WHITELIST);
        for (IPluginConfig elementConfig : whitelistConfig.getPluginConfigSet()) {
            final Collection<String> attributes = elementConfig.containsKey(ATTRIBUTES) ?
                    Arrays.asList(elementConfig.getStringArray(ATTRIBUTES)) : Collections.<String>emptyList();
            final String configName = elementConfig.getName();
            final int offset = configName.lastIndexOf('.');
            final String elementName = offset != -1 ? configName.substring(offset+1) : configName;
            final Element element = new Element(elementName, attributes);
            whitelist.put(element.name, element);
        }
        if (context != null) {
            context.registerService(this, IHtmlCleanerService.class.getName());
        }
    }

    @Override
    public String clean(final String value) throws Exception {
        HtmlCleaner cleaner = createCleaner();
        final TagNode node = cleaner.clean(value);
        if (filter(node) == null) {
            return "";
        }
        return serialize(node, cleaner.getProperties());
    }

    private TagNode filter(final TagNode node) {
        if (node.getName() != null && !whitelist.containsKey(node.getName())) {
            return null;
        }
        final Element element = whitelist.get(node.getName());
        final Map<String, String> attributes = node.getAttributes();
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            final String attributeName = attribute.getKey();
            final String attributeValue = attribute.getValue();
            if (!element.attributes.contains(attributeName)) {
                attributes.remove(attribute.getKey());
                continue;
            }
            final String value = escaper.escapeText(attributeValue.toLowerCase().trim());
            if (value.startsWith(JAVASCRIPT_PROTOCOL)) {
                attributes.put(attributeName, "");
            }
        }
        node.setAttributes(attributes);
        for (TagNode childNode : node.getChildTags()) {
            if (filter(childNode) == null) {
                node.removeChild(childNode);
            }
        }
        return node;
    }

    private String serialize(final TagNode html, final CleanerProperties properties) throws IOException {
        final Serializer serializer = createSerializer(properties);
        final StringWriter writer = new StringWriter();
        serializer.write(html, writer, charset);
        return writer.getBuffer().toString().trim();
    }

    private HtmlCleaner createCleaner() {
        final HtmlCleaner cleaner = new HtmlCleaner();
        final CleanerProperties properties = cleaner.getProperties();
        properties.setOmitHtmlEnvelope(true);
        properties.setTranslateSpecialEntities(false);
        properties.setOmitXmlDeclaration(true);
        properties.setOmitComments(omitComments);
        return cleaner;
    }

    private Serializer createSerializer(final CleanerProperties properties) {
        switch (serializer) {
            case PRETTY : return new PrettyHtmlSerializer(properties);
            case COMPACT : return new CompactHtmlSerializer(properties);
            default : return new SimpleHtmlSerializer(properties);
        }
    }

    private static final class Element implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name;
        private final Collection<String> attributes;
        private Element(final String name, final Collection<String> attributes) {
            this.name = name;
            this.attributes = attributes;
        }
    }

    private static class TextEscaper extends CompactHtmlSerializer {
        private TextEscaper() {
            super(new CleanerProperties() {{
                setRecognizeUnicodeChars(true);
                setTranslateSpecialEntities(true);
            }});
        }

        @Override
        protected String escapeText(final String s) {
            return super.escapeText(s);
        }
    }
}
