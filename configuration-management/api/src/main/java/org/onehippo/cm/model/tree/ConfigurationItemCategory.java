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
package org.onehippo.cm.model.tree;

/**
 * Defines the category of a ConfigurationItem, which determines its handling during various processing operations.
 */
public enum ConfigurationItemCategory {

    /**
     * Configuration data that is expected to be provided in detail by Hippo CMS projects and may include metadata.
     */
    CONFIG,

    /**
     * Content data that may be provided as "seed data", but is expected to be changed by users of Hippo CMS.
     */
    CONTENT,

    /**
     * System data that is generated dynamically by a running Hippo CMS for internal purposes only.
     */
    SYSTEM;

    public final String toString() {
        return name().toLowerCase();
    }

}
