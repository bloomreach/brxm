package org.onehippo.cm.backend;

import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;

/**
 * VerifiedReferenceValue is a placeholder implementation/decorator for resolved and validated {@link ValueType#REFERENCE}
 * and {@link ValueType#WEAKREFERENCE} {@link Value} instances to make it easier to compare and apply such properties
 * in the {@link ConfigurationPersistenceService}.
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
