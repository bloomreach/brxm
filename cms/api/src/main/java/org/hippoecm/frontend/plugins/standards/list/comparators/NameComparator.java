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
package org.hippoecm.frontend.plugins.standards.list.comparators;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;

public class NameComparator extends NodeComparator {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(JcrNodeModel o1, JcrNodeModel o2) {
        String name1 = new NodeTranslator(o1).getNodeName().getObject();
        String name2 = new NodeTranslator(o2).getNodeName().getObject();

        int result;
        if ((result = String.CASE_INSENSITIVE_ORDER.compare(name1, name2)) == 0) {
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
            } catch (RepositoryException e) {
            }
        } else {
            return result;
        }
        return 0;
    }

}
