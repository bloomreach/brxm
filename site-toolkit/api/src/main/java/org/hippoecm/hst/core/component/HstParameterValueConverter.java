/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.component;

import org.hippoecm.hst.core.request.ParameterConfiguration;

/**
 * Implementations of this interface are a utility class for converting String values to some object of type Class<?>
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
