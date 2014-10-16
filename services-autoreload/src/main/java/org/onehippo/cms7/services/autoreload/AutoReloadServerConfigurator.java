/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.autoreload;

import javax.websocket.server.ServerEndpointConfig;

/**
 * Ensures that the same instance of the {@link org.onehippo.cms7.services.autoreload.AutoReloadServer}
 * is used for each web socket connection.
 */
public class AutoReloadServerConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public <T> T getEndpointInstance(final Class<T> endpointClass) throws InstantiationException {
        if (endpointClass.equals(AutoReloadServer.class)) {
            return (T) AutoReloadServer.getInstance();
        }
        throw new InstantiationException("Cannot create instance of " + endpointClass);
    }

}
