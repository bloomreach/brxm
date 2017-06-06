/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

import java.math.BigDecimal;

/**
 * Abstract {@link ValueMap} base class.
 */
public abstract class AbstractValueMap implements ValueMap {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public final <T> T get(String name, Class<T> type) {
        final Object value = get(name, (T) null);

        if (value != null && type != null) {
            if (value instanceof String) {
                if (type == Integer.class) {
                    return (T) Integer.valueOf((String) value);
                } else if (type == Long.class) {
                    return (T) Long.valueOf((String) value);
                } else if (type == Double.class) {
                    return (T) Double.valueOf((String) value);
                } else if (type == Boolean.class) {
                    return (T) Boolean.valueOf((String) value);
                } else if (type == BigDecimal.class) {
                    return (T) new BigDecimal((String) value);
                }
            }

            if (!type.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("The type doesn't match with the value type: " + value.getClass());
            }
        }

        return (T) value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final <T> T get(String name, T defaultValue) {
        if (name == null) {
            throw new IllegalArgumentException("The name must not be a null.");
        }

        final Object value = get(name);
        return (value != null) ? (T) value : defaultValue;
    }
}
