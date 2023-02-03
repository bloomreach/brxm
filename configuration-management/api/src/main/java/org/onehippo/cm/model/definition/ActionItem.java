/*
 *  Copyright 2017-2023 Bloomreach
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
package org.onehippo.cm.model.definition;

import org.onehippo.cm.model.path.JcrPath;

/**
 * Represents an action item, which describes a bootstrap behavior {@link ActionType} for a specific path.
 */
public interface ActionItem {
    /**
     * @return the JCR node path to which this action applies
     */
    JcrPath getPath();

    /**
     * @return the {@link ActionType} that should be applied to the node at {@link #getPath()}
     */
    ActionType getType();

}
