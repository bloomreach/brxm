/*
 *  Copyright 2017-2022 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.TagNode;
import org.onehippo.cms7.services.htmlprocessor.serialize.CharacterReferenceNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class AllowlistHtmlFilter implements HtmlFilter {

    private static final Logger log = LoggerFactory.getLogger(AllowlistHtmlFilter.class);

    private static final String JAVASCRIPT_PROTOCOL = "javascript:";
    private static final String DATA_PROTOCOL = "data:";
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final Map<String, Element> elements = new HashMap<>();

    public AllowlistHtmlFilter() {
    }

    public AllowlistHtmlFilter(final List<Element> allowlist) {
        if (allowlist != null) {
            allowlist.forEach(this::add);
        }
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
            // if element is not in allowlist, ignore it, unless the node name is null which indicates
            // an omitted HTML envelope
            return null;
        }
        if (nodeName != null) {
            processAttributes(node);
        }

        for (final TagNode childNode : node.getChildTags()) {
            if (apply(childNode) == null) {
                node.removeChild(childNode);
            }
        }
        return node;
    }

    private void processAttributes(final TagNode node) {
        final Element allowedElement = elements.get(node.getName());

        final Map<String, String> attributes =
                node.getAttributes().entrySet().stream()
                .filter(attribute -> allowedElement.hasAttribute(attribute.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, attribute -> {
                    final String value = attribute.getValue();
                    final String trimmedValue = value.toLowerCase().trim();
                    final String normalizedValue = CharacterReferenceNormalizer.normalizeAttributeContent(trimmedValue);
                    final String cleanedValue = cleanWhitespace(normalizedValue);

                    if (allowedElement.isOmitJsProtocol() && cleanedValue.startsWith(JAVASCRIPT_PROTOCOL)) {
                        return StringUtils.EMPTY;
                    }

                    if (allowedElement.isOmitDataProtocol() && cleanedValue.startsWith(DATA_PROTOCOL)) {
                        return StringUtils.EMPTY;
                    }

                    return value;
                }));

        if (allowedElement.isSecureTargetBlankLinks()) {
            secureTargetBlankLinks(node, attributes);
        }

        node.setAttributes(attributes);
    }

    private static String cleanWhitespace(final String value) {
        return WHITESPACE.matcher(value).replaceAll("");
    }

    private void secureTargetBlankLinks(final TagNode node, final Map<String, String> attributes) {
        if (!node.getName().equals("a")) {
            return;
        }

        if (!attributes.containsKey("target")) {
            return;
        }

        if (!attributes.get("target").equalsIgnoreCase("_blank")) {
            return;
        }

        final List<String> relAttrValues = Lists.newArrayList("noopener", "noreferrer");

        String currentValue = attributes.get("rel");
        if (StringUtils.isNotBlank(currentValue)) {
            final String[] existingValues = currentValue.trim().split("\\s+");
            for (final String existingValue : existingValues) {
                if (!relAttrValues.contains(existingValue)) {
                    relAttrValues.add(existingValue);
                }
            }
        }

        attributes.put("rel", String.join(" ", relAttrValues));
    }
}
