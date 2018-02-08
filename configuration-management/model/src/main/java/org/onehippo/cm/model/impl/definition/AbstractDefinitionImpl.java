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
package org.onehippo.cm.model.impl.definition;

import org.onehippo.cm.model.definition.Definition;
import org.onehippo.cm.model.impl.source.SourceImpl;

public abstract class AbstractDefinitionImpl<S extends SourceImpl> implements Definition<S> {

    private S source;

    public AbstractDefinitionImpl(final S source) {
        this.source = source;
    }

    @Override
    public S getSource() {
        return source;
    }

    @Override
    public String getOrigin() {
        return source.getOrigin();
    }
}
