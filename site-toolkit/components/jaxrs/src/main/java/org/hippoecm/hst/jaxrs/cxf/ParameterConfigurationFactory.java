/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.cxf;

import org.apache.cxf.message.Message;
import org.hippoecm.hst.core.request.ParameterConfiguration;

/**
 * Creates a {@link ParameterConfiguration} instance based on CXF {@link Message} instance in the JAX-RS invocation context.
 */
public interface ParameterConfigurationFactory {

    /**
     * Resolves a {@link ParameterConfiguration} instance based current request context as well as the given {@link Message}.
     * @param message CXF message object
     * @return a resolved {@link ParameterConfiguration} instance
     */
    ParameterConfiguration create(Message message);

}
