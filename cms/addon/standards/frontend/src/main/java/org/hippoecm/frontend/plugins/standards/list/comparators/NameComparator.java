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

import java.util.Comparator;

import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;

public class NameComparator implements Comparator<JcrNodeModel>, IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

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
            String name1 = n1.getName();
            String name2 = n2.getName();
            return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
        } catch (RepositoryException e) {
            return 0;
        }
    }

}
