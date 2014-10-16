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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Auto-reload service based on web sockets.
 */
class AutoReloadServiceImpl implements AutoReloadService {

    private final AutoReloadServiceConfig config;
    private final AutoReloadScriptLoader scriptLoader;
    private final AutoReloadServer autoReloadServer;
    private final AtomicBoolean enabled;
    private final String cachedJavaScript;

    AutoReloadServiceImpl(final AutoReloadServiceConfig config, final AutoReloadScriptLoader scriptLoader, final AutoReloadServer autoReloadServer) {
        this.config = config;
        this.scriptLoader = scriptLoader;
        this.autoReloadServer = autoReloadServer;
        enabled = new AtomicBoolean(config.isEnabled());
        cachedJavaScript = scriptLoader.getJavaScript();
    }

    @Override
    public boolean isEnabled() {
        return cachedJavaScript != null && enabled.get() && config.isEnabled();
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
        enabled.set(isEnabled);
    }

    @Override
    public String getJavaScript() {
        return cachedJavaScript;
    }

    @Override
    public void broadcastPageReload() {
        if (isEnabled()) {
            autoReloadServer.broadcastPageReload();
        }
    }
}
