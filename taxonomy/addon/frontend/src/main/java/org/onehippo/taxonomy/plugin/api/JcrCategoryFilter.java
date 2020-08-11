/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.api;

import java.io.Serializable;

import org.hippoecm.repository.api.HippoSession;
import org.onehippo.taxonomy.plugin.model.JcrCategory;

/**
 * Filter definition for loading categories into the taxonomy tree model. Make sure that implementations are serializable
 */
public interface JcrCategoryFilter extends Serializable {

    /**
     * Load this category or not into the taxonomy tree?
     *
     * @return true if the category may be part of the taxonomy tree, otherwise false.
     */
    boolean apply(JcrCategory category, HippoSession session);
}