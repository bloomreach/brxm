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
package org.onehippo.cms.services.validation.util;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.BaseToken;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import com.google.common.base.CharMatcher;

public class HtmlUtils implements Serializable {

    private static final String[] VALID_ELEMENTS = new String[]{"applet", "embed", "form", "iframe", "img", "object"};

    public static boolean isEmpty(final String html) {
        if (StringUtils.isBlank(html)) {
            return true;
        }

        final HtmlCleaner cleaner = getHtmlCleaner();
        final TagNode parsedHtml = cleaner.clean(html);
        return isEmpty(parsedHtml);
    }

    private static HtmlCleaner getHtmlCleaner() {
        final HtmlCleaner cleaner = new HtmlCleaner();
        final CleanerProperties properties = cleaner.getProperties();
        properties.setOmitXmlDeclaration(true);
        properties.setOmitHtmlEnvelope(true);
        properties.setOmitComments(true);
        properties.setNamespacesAware(false);
        properties.setDeserializeEntities(true);
        return cleaner;
    }

    private static boolean isEmpty(final TagNode node) {
        for (final BaseToken item : node.getAllChildren()) {
            if (item instanceof TagNode) {
                final TagNode childNode = (TagNode) item;
                if (Arrays.stream(VALID_ELEMENTS).anyMatch(e -> childNode.getName().equalsIgnoreCase(e))
                        || CharMatcher.invisible().negate().matchesAnyOf(childNode.getText())
                        || !isEmpty(childNode)) {
                    return false;
                }
            } else if (item instanceof ContentNode) {
                if (CharMatcher.invisible().negate().matchesAnyOf(((ContentNode) item).getContent())) {
                    return false;
                }
            }
        }
        return true;
    }
}
