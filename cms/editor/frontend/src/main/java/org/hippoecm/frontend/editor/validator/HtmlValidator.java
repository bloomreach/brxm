/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.validator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.CharMatcher;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.validation.ValidatorMessages;
import org.htmlcleaner.BaseToken;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class HtmlValidator implements IClusterable {

    private static final long serialVersionUID = 1L;

    public static final String[] VALID_ELEMENTS = new String[]{"img", "object", "embed", "form", "applet", "iframe"};

    public Set<String> validateNonEmpty(String html) {
        final Set<String> result = new HashSet<>();
        final HtmlCleaner cleaner = new HtmlCleaner();
        final CleanerProperties properties = cleaner.getProperties();
        properties.setOmitXmlDeclaration(true);
        properties.setOmitHtmlEnvelope(true);
        properties.setOmitComments(true);
        properties.setNamespacesAware(false);
        properties.setDeserializeEntities(true);
        if (isEmpty(cleaner.clean(html))) {
            result.add(ValidatorMessages.HTML_IS_EMPTY);
        }
        return result;
    }

    private boolean isEmpty(final TagNode node) {
        for (BaseToken item : node.getAllChildren()) {
            if (item instanceof TagNode) {
                final TagNode childNode = (TagNode)item;
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
