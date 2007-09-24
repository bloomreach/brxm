/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.servicing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

class NodeView {
    final static private String SVN_ID = "$Id$";

    Map<String,String> view;

    NodeView(NodeView current, Property modifyFacets, Property modifyValues, Property modifyModes)
      throws RepositoryException {
        view = new HashMap<String,String>();
        if(modifyFacets.getDefinition().isMultiple() || modifyValues.getDefinition().isMultiple() ||
           modifyModes.getDefinition().isMultiple()) {
            Value[] newFacets = modifyFacets.getValues();
            Value[] newValues = modifyValues.getValues();
            Value[] newModes  = modifyModes.getValues();
            if(newFacets.length != newValues.length || newFacets.length != newModes.length)
                throw new RepositoryException("malformed definition of faceted selection");
            for(int i=0; i<newFacets.length; i++)
                modifyView(newFacets[i].getString(), newValues[i].getString(), newModes[i].getString());
        } else {
            if(modifyFacets.getDefinition().isMultiple() && modifyValues.getDefinition().isMultiple() &&
               modifyModes.getDefinition().isMultiple()) {
                modifyView(modifyFacets.getString(), modifyValues.getString(), modifyModes.getString());
            } else
                throw new RepositoryException("malformed definition of faceted selection");
        }
    }

    private void modifyView(String facet, String value, String mode) {
        if(mode.equals("stick") || mode.equals("select")) {
            view.put(facet, value);
        } else if(mode.equals("clear")) {
            view.remove(facet);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append("[");
        boolean first = true;
        for(Map.Entry<String,String> entry : view.entrySet()) {
            if(!first) {
                sb.append(",");
                first = false;
            }
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
        }
        sb.append("]");
        return new String(sb);
    }

    Node getNode(NodeIterator iter) {
        Node rtValue;
        while(iter.hasNext()) {
            rtValue = match(iter.nextNode());
            if(rtValue != null)
                return rtValue;
        }
        return null;
    }

    Node[] getNodes(Node[] nodes) {
        ArrayList<Node> filtered = new ArrayList<Node>(nodes.length);
        for(int i=0; i<nodes.length; i++) {
            Node node = match(nodes[i]);
            if(node != null)
                filtered.add(node);
        }
        return filtered.toArray(new Node[filtered.size()]);
    }

    NodeIterator getNodes(NodeIterator iter) {
        return this . new MatchingNodeIterator(iter);
    }

    Node match(Node node) {
        int i;
        try {
            for(Map.Entry<String,String> entry : view.entrySet()) {
                String facet = entry.getKey();
                String value = entry.getValue();
                if(node.hasProperty(facet)) {
                    if(value != null && !value.equals("") && !value.equals("*"))
                        if(!node.getProperty(facet).getString().equals(value)) {
                            return null;
                        }
                } else {
                    return null;
                }
            }
        } catch(PathNotFoundException ex) {
            return null;
        } catch(RepositoryException ex) {
            return null;
        }
        return node;
    }

    class MatchingNodeIterator implements NodeIterator {
        NodeIterator iterator;
        Node lookahead;
        int absPosition;

        MatchingNodeIterator(NodeIterator iterator) {
            this.iterator = iterator;
            lookahead = null;
            absPosition = 0;
        }

        public boolean hasNext() {
            if (lookahead == null) {
                try {
                    do {
                        lookahead = match(iterator.nextNode());
                    } while(lookahead == null);
                } catch(NoSuchElementException ex) {
                    return false;
                }
                return true;
            } else
                return true;
        }

        public Object next() {
            Node rtValue = null;
            if (lookahead != null) {
                rtValue = lookahead;
                lookahead = null;
            } else {
                do {
                    lookahead = match(iterator.nextNode());
                } while(lookahead == null);
            }
            ++absPosition;
            return rtValue;
        }

        public Node nextNode() {
            return (Node) next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void skip(long skipNum) {
            while (skipNum-- > 0) {
                ++absPosition;
                iterator.next();
            }
        }

        public long getSize() {
            return -1;
        }

        public long getPosition() {
            return absPosition;
        }
    }
}
