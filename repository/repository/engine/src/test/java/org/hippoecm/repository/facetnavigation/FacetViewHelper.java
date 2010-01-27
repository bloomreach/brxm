/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.facetnavigation;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class FacetViewHelper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static void traverse(Node navigation) throws RepositoryException {
        traverse(navigation, "", 0);
    }

    public static void traverse(Node navigation, String indent, int depth) throws RepositoryException {
        depth++;
        if (depth > 12) {
            return;
        }
        String countStr = "";
        if (navigation.hasProperty("hippo:count")) {
            countStr = " [" + navigation.getProperty("hippo:count").getLong() + "]";
        }
        if (navigation.hasProperty("hippo:price")) {
            countStr += " = " + navigation.getProperty("hippo:price").getDouble() + "$ ";
        }
        if (navigation.hasProperty("hippo:travelled")) {
            countStr += " = " + navigation.getProperty("hippo:travelled").getLong() + "km ";
        }
        System.out.println(indent + navigation.getName() + countStr);

        if(!navigation.getName().equals("hippo:resultset")) { 
            NodeIterator it = navigation.getNodes();
            indent += "\t";
            while (it.hasNext()) {
                traverse(it.nextNode(), indent, depth);
            }
        }
    }
}
