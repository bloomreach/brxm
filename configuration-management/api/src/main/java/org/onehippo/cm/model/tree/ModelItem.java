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
package org.onehippo.cm.model.tree;


import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;

/**
 * Represents the (potential) state of a JCR Node or Property as specified in either a ConfigurationItem or
 * DefinitionItem tree.
 */
public interface ModelItem<N extends ModelNode> {

    /**
     * @return the name of this node or property -- not expected to be JCR encoded
     */
    String getName();

    /**
     * @return the name of this node or property -- not expected to be JCR encoded
     */
    JcrPathSegment getJcrName();

    /**
     * @return the full path of this node or property (including a segment for {@link #getName()})
     */
    String getPath();

    /**
     * @return the full path of this node or property (including a segment for {@link #getName()})
     */
    JcrPath getJcrPath();

    /**
     * @return the parent node for this item, or null if this item represents the root node
     */
    N getParent();

    /**
     * @return true iff this item represents the JCR root node
     */
    boolean isRoot();

    /**
     * @return a String describing the source of this item for error-reporting purposes
     */
    String getOrigin();

}
