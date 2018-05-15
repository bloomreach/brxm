/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.htmlcleaner.TagNode;
import org.onehippo.cms7.services.htmlprocessor.serialize.CharacterReferenceNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhitelistHtmlFilter implements HtmlFilter {

    public static final Logger log = LoggerFactory.getLogger(WhitelistHtmlFilter.class);

    private static final String JAVASCRIPT_PROTOCOL = "javascript";
    private static final String DATA_PROTOCOL = "data";
    private static final Pattern CRLFTAB = Pattern.compile("[\r\n\t]");

    private final Map<String, Element> elements = new HashMap<>();
    private final boolean omitJavascriptProtocol;

    public WhitelistHtmlFilter() {
        omitJavascriptProtocol = true;
    }

    public WhitelistHtmlFilter(final List<Element> whitelist, final boolean omitJavascriptProtocol) {
        if (whitelist != null) {
            whitelist.forEach(this::add);
        }
        this.omitJavascriptProtocol = omitJavascriptProtocol;
    }

    @Override
    public void add(final Element element) {
        if (elements.containsKey(element.getName())) {
            log.warn("Found existing definition of element {}, replacing previous definition.", element.getName());
        }
        elements.put(element.getName(), element);
    }

    @Override
    public TagNode apply(final TagNode node) {
        final String nodeName = node.getName();
        if (!elements.containsKey(nodeName) && nodeName != null) {
            // if element is not whitelisted, ignore it, unless the node name is null which indicates
            // an omitted HTML envelope
            return null;
        }
        if (nodeName != null) {
            filterAttributes(node);
        }

        for (final TagNode childNode : node.getChildTags()) {
            if (apply(childNode) == null) {
                node.removeChild(childNode);
            }
        }
        return node;
    }

    private void filterAttributes(final TagNode node) {
        final Element allowedElement = elements.get(node.getName());

        final Map<String, String> attributes =
                node.getAttributes().entrySet().stream()
                .filter(attribute -> allowedElement.hasAttribute(attribute.getKey()))
                .collect(Collectors.toMap(attribute -> attribute.getKey(), attribute -> {
                    final String value = attribute.getValue();
                    final String normalizedValue =
                            cleanCRLFTAB(CharacterReferenceNormalizer.normalizeAttributeContent(value.toLowerCase().trim()));
                    if (omitJavascriptProtocol &&
                            (normalizedValue.startsWith(JAVASCRIPT_PROTOCOL) ||
                                    checkDataAttrValue(node.getName(), attribute.getKey(), normalizedValue))) {
                        return "";
                    }
                    return value;
                }));
        node.setAttributes(attributes);
    }

    private static String cleanCRLFTAB(final String value) {
        return CRLFTAB.matcher(value).replaceAll("");
    }

    private boolean checkDataAttrValue(final String tagName, final String attrName, final String attrValue) {
        return attrValue.startsWith(DATA_PROTOCOL)
                ? ("a".equals(tagName) && "href".equals(attrName)) || ("object".equals(tagName) && "data".equals(attrName))
                : false;
    }
}
