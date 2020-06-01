/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.comparators;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeNameModel;

public class NameComparator extends NodeComparator {

    private static final NameComparator INSTANCE = new NameComparator();

    private NameComparator() {
    }

    public static NameComparator getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(JcrNodeModel o1, JcrNodeModel o2) {
        String name1 = new NodeNameModel(o1).getObject();
        String name2 = new NodeNameModel(o2).getObject();

        int nameCompare = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
        if (nameCompare == 0) {
            try {
                Node n1 = o1.getNode();
                Node n2 = o2.getNode();
                if (n1 == null) {
                    if (n2 == null) {
                        return 0;
                    }
                    return 1;
                } else if (n2 == null) {
                    return -1;
                }
                return n1.getIndex() - n2.getIndex();
            } catch (RepositoryException ignored) {
            }
        } else {
            return nameCompare;
        }
        return 0;
    }

}
