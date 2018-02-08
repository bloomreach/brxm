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

import org.onehippo.cm.model.definition.TreeDefinition;
import org.onehippo.cm.model.source.Source;

/**
 * Represents the (potential) state of a JCR Node or Property as specified in a DefinitionItem tree.
 */
public interface DefinitionItem extends ModelItem {

    @Override
    DefinitionNode getParent();

    /**
     * @return the ContentDefinition (or ConfigDefinition) in which this item is defined
     */
    TreeDefinition<? extends Source> getDefinition();

    /**
     * @return an object representing a precise position in a Source where this item is defined
     */
    SourceLocation getSourceLocation();

    /**
     * @return the category of this item, or null if no specific category has been specified for this item
     */
    ConfigurationItemCategory getCategory();

}
