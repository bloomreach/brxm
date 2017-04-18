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
package org.onehippo.cms7.services.processor.html.visit;

import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;

public interface Tag {

    String getName();

    String getAttribute(final String name);

    void addAttribute(final String name, final String value);

    boolean hasAttribute(final String name);

    void removeAttribute(final String name);

    static Tag from(final TagNode tagNode) {
        return tagNode != null ? new TagNodeDelegate(tagNode) : null;
    }

    static Tag from(final HtmlNode htmlNode) {
        return htmlNode instanceof TagNode ? from((TagNode) htmlNode) : null;
    }

    class TagNodeDelegate implements Tag {

        private final TagNode tagNode;

        TagNodeDelegate(final TagNode tagNode) {
            this.tagNode = tagNode;
        }

        @Override
        public String getName() {
            return tagNode.getName();
        }

        @Override
        public String getAttribute(final String name) {
            return tagNode.getAttributeByName(name);
        }

        @Override
        public void addAttribute(final String name, final String value) {
            tagNode.addAttribute(name, value);
        }

        @Override
        public boolean hasAttribute(final String name) {
            return tagNode.hasAttribute(name);
        }

        @Override
        public void removeAttribute(final String name) {
            tagNode.removeAttribute(name);
        }
    }
}
