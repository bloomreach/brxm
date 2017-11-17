/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.event;

/**
 * Rebuild events should be posted on the event bus if an event occurred which requires a rebuild of the project.
 * This could be the execution of some instruction during the installation of a plugin, or a change made to the
 * project by a 'tool'.
 */
public class RebuildEvent implements PluginEvent {

    private static final long serialVersionUID = 1L;

    private final String pluginId;
    private final String message;

    public RebuildEvent(final String pluginId, final String message) {
        this.message = message;
        this.pluginId = pluginId;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public String getPluginId() {
        return pluginId;
    }
}
