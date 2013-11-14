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
 * Decorator for 'img' tags that adds a 'data-uuid' attribute based on the provided 'src' attribute of the image.
 * The part of the 'src' attribute before the first slash ('/') is supposed to be the name of a child node of the
 * provided document node of type 'hippo:facetselect'. If such a child node exists, the 'data-uuid' attribute will be set
 * to the docbase of that facetselect node (i.e. the UUID of the node it refers to). Otherwise, the 'data-uuid' attribute
 * will be empty.
 */
class ImgAddUuidDecorator extends InternalLinkDecorator {

    private final Node documentNode;

    ImgAddUuidDecorator(Node documentNode) {
        super("src");
        this.documentNode = documentNode;
    }

    @Override
    public String internalLink(final String src) {
        final String childFacetNodeName = StringUtils.substringBefore(src, "/");
        final String uuidOrNull = RichTextFacetHelper.getChildDocBaseOrNull(this.documentNode, childFacetNodeName);

        if (uuidOrNull == null) {
            // Child facet does not exist; assume the src is referring to something else and should be returned as-is
            return "src=\"" + src + "\"";
        }

        return "src=\"" + src + "\" data-uuid=\"" + uuidOrNull + "\"";
    }

}
