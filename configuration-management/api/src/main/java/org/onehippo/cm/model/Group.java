/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model;

import java.util.List;

/**
 * Represents the first level of a three-level hierarchy used for managing dependency relationships between
 * {@link Module}s. This is intended to equate conceptually to the level of Maven group IDs in that dependency
 * management system.
 */
public interface Group extends OrderableByName {

    /**
     * @return the name of the HCM site to which this group belongs, or null if this is part of the core model
     * @see Module#getHcmSiteName()
     */
    String getHcmSiteName();

    /**
     * @return The immutable list of {@link Project}s currently in this Group.
     */
    List<? extends Project> getProjects();

}
