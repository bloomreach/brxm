/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.model.definition;

import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.source.ConfigSource;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.source.SourceType;

/**
 * Represents a location of a Hippo CMS webfile bundle within the containing {@link Module}. Definitions of
 * this type are found in a {@link Source} of type {@link SourceType#CONFIG}.
 */
public interface WebFileBundleDefinition<S extends ConfigSource> extends Definition<S> {
    /**
     * @return the name of a single Hippo CMS webfile bundle
     */
    String getName();
}
