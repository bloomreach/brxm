/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.openui.extensions;

import java.io.Serializable;

/**
 * A UI Extension of the CMS.
 */
public interface UiExtension extends Serializable {

    int DEFAULT_INITIAL_HEIGHT_IN_PIXELS = 150;

    /**
     * @return identifier of the extension. Must be unique across all extensions.
     */
    String getId();

    /**
     * @return human-readable name of the extension. Shown in the UI.
     */
    String getDisplayName();

    /**
     * @return the extension point for this extension.
     */
    UiExtensionPoint getExtensionPoint();

    /**
     * @return the URL that loads the extension.
     */
    String getUrl();

    /**
     * @return the initial height in pixels. Only used by {@link UiExtensionPoint#DOCUMENT_FIELD} extensions.
     */
    int getInitialHeightInPixels();

    /**
     * @return the configuration to pass to the extension.
     */
    String getConfig();

}
