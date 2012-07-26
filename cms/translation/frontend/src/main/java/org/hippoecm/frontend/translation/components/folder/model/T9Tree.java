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
package org.hippoecm.frontend.translation.components.folder.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Tree interface as an abstraction over the (translated) folder structure.
 * <p>
 * The data model needed by the (Ext-JS) view is decoupled from the JCR implementation
 * by this class; this enables quick turnaround in the testing of the view in the test war.
 */
public abstract class T9Tree {

    public abstract T9Node getRoot();

    public abstract List<T9Node> getChildren(String nodeId);

    public abstract List<T9Node> getSiblings(String t9Id);

    public abstract T9Node getNode(String id);

    public List<T9Node> getPath(String id) {
        T9Node node = getNode(id);
        List<T9Node> path = new LinkedList<T9Node>();
        if (node != null) {
            do {
                path.add(node);
                node = node.getParent();
            } while (node != null);
            Collections.reverse(path);
        }
        return path;
    }

}
