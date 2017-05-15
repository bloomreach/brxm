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
package org.onehippo.cm.backend;

import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;

/**
 * VerifiedReferenceValue is a placeholder implementation/decorator for resolved and validated {@link ValueType#REFERENCE}
 * and {@link ValueType#WEAKREFERENCE} {@link Value} instances to make it easier to compare and apply such properties
 * in the {@link ConfigService}.
 */
public final class VerifiedReferenceValue implements Value {

    private final DefinitionProperty parent;
    private final ValueType type;
    private final String uuid;

    public VerifiedReferenceValue(final Value value, final String uuid) {
        this.parent = value.getParent();
        this.type = value.getType();
        if (type == ValueType.REFERENCE || type == ValueType.WEAKREFERENCE) {
            this.uuid = uuid;
        } else {
            throw new IllegalArgumentException("value type must be " +
                    ValueType.REFERENCE.name() + " or " + ValueType.WEAKREFERENCE.name());
        }
    }

    @Override
    public Object getObject() {
        return uuid;
    }

    @Override
    public String getString() {
        return uuid;
    }

    @Override
    public ValueType getType() {
        return type;
    }

    @Override
    public boolean isResource() {
        return false;
    }

    @Override
    public boolean isPath() {
        return false;
    }

    @Override
    public DefinitionProperty getParent() {
        return parent;
    }
}
