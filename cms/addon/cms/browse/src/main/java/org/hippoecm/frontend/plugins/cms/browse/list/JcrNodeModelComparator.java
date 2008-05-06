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
package org.hippoecm.frontend.plugins.cms.browse.list;

import java.io.Serializable;
import java.util.Comparator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class JcrNodeModelComparator implements Comparator<JcrNodeModel>, Serializable {
    private static final long serialVersionUID = 1L;

    String sortby;

    public JcrNodeModelComparator(String sortby) {
        this.sortby = sortby;
    }

    public int compare(JcrNodeModel o1, JcrNodeModel o2) {
        try {
            HippoNode n1 = o1.getNode();
            HippoNode n2 = o2.getNode();
            if (n1 == null) {
                if (n2 == null) {
                    return 0;
                }
                return 1;
            } else if (o2 == null) {
                return -1;
            }
            if (sortby.equals("name")) {
                String name1 = n1.getName();
                String name2 = n2.getName();
                return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
            } else if (sortby.equals("state")) {
                HippoNode variant = (HippoNode) (n1.getNode(n1.getName()));
                Node canonicalNode = variant.getCanonicalNode();
                String state1 = "unknown";
                if (canonicalNode.hasProperty("hippostd:stateSummary")) {
                    state1 = canonicalNode.getProperty("hippostd:stateSummary").getString();
                }
                variant = (HippoNode) (n2.getNode(n2.getName()));
                canonicalNode = variant.getCanonicalNode();
                String state2 = "unknown";
                if (canonicalNode.hasProperty("hippostd:stateSummary")) {
                    state2 = canonicalNode.getProperty("hippostd:stateSummary").getString();
                }
                return String.CASE_INSENSITIVE_ORDER.compare(state1, state2);
            } else if (sortby.equals("path")) {
                String path1 = n1.getPath();
                String path2 = n2.getPath();
                return String.CASE_INSENSITIVE_ORDER.compare(path1, path2);
            } else if (sortby.equals("islocked")) {
                String isLocked1 = String.valueOf(n1.isLocked());
                String isLocked2 = String.valueOf(n2.isLocked());
                return String.CASE_INSENSITIVE_ORDER.compare(isLocked1, isLocked2);
            } else if (sortby.equals(JcrConstants.JCR_PRIMARYTYPE)) {
                String label1 = getLabel1(n1);
                String label2 = getLabel1(n2);
                return String.CASE_INSENSITIVE_ORDER.compare(label1, label2);
            } else {
                if(n1.hasProperty(sortby) && n2.hasProperty(sortby)) {
                    Property prop1 = n1.getProperty(sortby);
                    Property prop2 = n1.getProperty(sortby);
                    if(prop1.getValue().getType() != prop2.getValue().getType()) {
                        // if multi-valued, exception will be thrown below but is not problem
                        return 0;
                    }
                    int type = prop1.getValue().getType();
                    switch(type) {
                        case  PropertyType.STRING:
                            return String.CASE_INSENSITIVE_ORDER.compare(prop1.getValue().getString(), prop2.getValue().getString());
                        case PropertyType.BOOLEAN:
                            return String.CASE_INSENSITIVE_ORDER.compare(String.valueOf(prop1.getValue().getBoolean()),String.valueOf(prop2.getValue().getBoolean()));
                        case PropertyType.DOUBLE:
                            return (int) (prop1.getValue().getDouble() - prop2.getValue().getDouble());
                        case PropertyType.DATE:
                            return prop1.getValue().getDate().after(prop2.getValue().getDate()) ? -1 : 1  ;
                       case PropertyType.LONG:
                           return (int) (prop1.getValue().getLong() - prop2.getValue().getLong());
                    default: 
                        return 0;
                    }
                } else {
                    return 0;
                }
            }
        } catch (RepositoryException e) {
            return 0;
        }
    }

    private String getLabel1(HippoNode n) throws RepositoryException {
        String label = "";
        if (n.isNodeType(HippoNodeType.NT_HANDLE)) {
            label = n.getPrimaryNodeType().getName();
            NodeIterator nodeIt = n.getNodes();
            while (nodeIt.hasNext()) {
                Node childNode = nodeIt.nextNode();
                if (childNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    label = childNode.getPrimaryNodeType().getName();
                    break;
                }
            }
            if (label.indexOf(":") > -1) {
                label = label.substring(label.indexOf(":") + 1);
            }
        } else {
            label = "folder";
        }
        return label;
    }

}
