/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.reviewedactions.list.comparators;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.comparators.NodeComparator;

public class StateComparator extends NodeComparator {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(JcrNodeModel o1, JcrNodeModel o2) {
        try {
            Node n1 = o1.getNode();
            Node n2 = o2.getNode();
            Node variant = n1.getNode(n1.getName());
            String state1 = "unknown";
            if (variant.hasProperty("hippostd:stateSummary")) {
                state1 = variant.getProperty("hippostd:stateSummary").getString();
            }
            variant = n2.getNode(n2.getName());
            String state2 = "unknown";
            if (variant.hasProperty("hippostd:stateSummary")) {
                state2 = variant.getProperty("hippostd:stateSummary").getString();
            }
            return String.CASE_INSENSITIVE_ORDER.compare(state1, state2);
        } catch (RepositoryException e) {
            return 0;
        }
    }

}
