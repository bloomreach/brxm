/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

/**
 * Represents an item in the Configuration tree, which may be a ConfigurationNode or ConfigurationProperty.
 * @param <D> the type of DefinitionItem expected from {@link #getDefinitions()}
 */
public interface ConfigurationItem<D extends DefinitionItem> extends ModelItem {

    /**
     * @return the parent node for this item
     */
    @Override
    ConfigurationNode getParent();

    /**
     * @return The <strong>ordered</strong> immutable {@link List} of {@link DefinitionItem}s that were used to
     * create this ConfigurationItem, or an empty immutable List if this instance was created directly without
     * using definitions. The order of items in this list describes the order of processing that was used when
     * merging the final state of this item.
     */
    List<D> getDefinitions();

}
