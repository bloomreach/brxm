/*
 * Copyright 2012-2022 Bloomreach
 */
package org.hippoecm.hst.core.component;

import org.hippoecm.hst.core.request.ParameterConfiguration;

/**
 * Implementations of this interface are a utility class for converting String values to some object of type {@code Class<?>}
 */
public interface HstParameterValueConverter {

    /**
     * @param parameterValue
     * @param returnType
     * @return the {@link String} <code>parameterValue</code> converted to the <code>returnType</code> class
     * @throws HstParameterValueConversionException when the conversion failed
     */
    Object convert(String parameterValue, Class<?> returnType) throws HstParameterValueConversionException;

    default Object convert(String parameterName, String parameterValue, ParameterConfiguration parameterConfiguration,  Class<?> returnType) {
        return convert(parameterValue, returnType);
    }
}
