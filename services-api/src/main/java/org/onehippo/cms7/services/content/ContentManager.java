/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.content;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.onehippo.cms7.services.contenttype.ContentTypes;

/**
 */
public interface ContentManager {

    Session getSession();
    ContentTypes getContentTypes();
    ValueFactory getValueFactory();
    ContentNode getContentNode(Node node);
    ContentNode getContentNodeByIdentifier(String identifier);
    ContentNode getContentNodeByPath(String path);
    RangeIterable<ContentNode> getContentNodesByIdentifiers(Iterable<String> identifiers);
    RangeIterable<ContentNode> getContentNodesByIdentifiers(Iterator<String> identifiers);
    RangeIterable<ContentNode> getContentNodesByPaths(Iterable<String> paths);
    RangeIterable<ContentNode> getContentNodesByPaths(Iterator<String> paths);
    RangeIterable<ContentNode> getContentNodes(Iterable<Node> nodes);
    RangeIterable<ContentNode> getContentNodes(NodeIterator nodes);
    void attach(ContentNode node);
    void write(ContentNode node);
    void write(Iterable<ContentNode> nodes);
    void write(Iterator<ContentNode> nodes);
}
