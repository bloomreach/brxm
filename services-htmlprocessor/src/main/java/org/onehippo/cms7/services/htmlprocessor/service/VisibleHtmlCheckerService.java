/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.service;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.BaseToken;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import com.google.common.base.CharMatcher;

import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;

public class VisibleHtmlCheckerService {

    private static final String[] DEFAULT_VISIBLE_ELEMENTS = {
            "applet", "embed", "form", "iframe", "img", "object", "table"
    };

    private final HtmlCleaner cleaner;
    private final String[] visibleElements;

    public VisibleHtmlCheckerService(final Node config) {
        visibleElements = getPropertyOrDefault(config, "visibleElements", DEFAULT_VISIBLE_ELEMENTS);
        cleaner = new HtmlCleaner();

        final CleanerProperties properties = cleaner.getProperties();
        properties.setOmitXmlDeclaration(true);
        properties.setOmitHtmlEnvelope(true);
        properties.setOmitComments(true);
        properties.setNamespacesAware(false);
        properties.setDeserializeEntities(true);
    }

    private static String[] getPropertyOrDefault(final Node node, final String propertyName, final String[] defaultValue) {
        try {
            return getMultipleStringProperty(node, propertyName, defaultValue);
        } catch (RepositoryException ignore) {
        }
        return defaultValue;
    }

    public boolean isVisible(final String html) {
        if (StringUtils.isBlank(html)) {
            return false;
        }

        final TagNode parsedHtml = cleaner.clean(html);
        return isVisible(parsedHtml);
    }

    private boolean isVisible(final TagNode node) {
        for (final BaseToken item : node.getAllChildren()) {
            if (item instanceof TagNode) {
                final TagNode childNode = (TagNode) item;
                if (Arrays.stream(visibleElements).anyMatch(e -> childNode.getName().equalsIgnoreCase(e))
                        || hasVisibleText(childNode.getText().toString())
                        || isVisible(childNode)) {
                    return true;
                }
            } else if (item instanceof ContentNode) {
                if (hasVisibleText(((ContentNode) item).getContent())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasVisibleText(final String text) {
        return CharMatcher.invisible().negate().matchesAnyOf(text);
    }
}
