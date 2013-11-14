/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.richtext.jcr;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;

/**
 * Decorator for the 'href' attribute of link ('a') tags that replaces the 'data-uuid' attribute of internal links
 * with an 'href' attribute set to the name of the child hippo:facetselect node of the document node.
 * When no such facetselect node exists, the 'href' attribute will be removed.
 */
class InternalLinkUuidToHrefDecorator extends InternalLinkDecorator {

    private final Node documentNode;

    InternalLinkUuidToHrefDecorator(Node documentNode) {
        super("href");
        this.documentNode = documentNode;
    }

    @Override
    public String internalLink(final String uuid) {
        final String facetNodeName = RichTextFacetHelper.getChildFacetNameOrNull(this.documentNode, uuid);
        if (facetNodeName == null) {
            return StringUtils.EMPTY;
        }
        return "href=\"" + facetNodeName + "\"";
    }

}
