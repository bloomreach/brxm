/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.extensions;

/**
 * An extension of the CMS.
 */
public interface CmsExtension {

    /**
     * @return identifier of the extension. Must be unique across all extensions.
     */
    String getId();

    /**
     * @return human-readable name of the extension. Shown in the UI.
     */
    String getDisplayName();

    /**
     * @return the context supported by this extension, i.e. the thing in the CMS this extension knows something
     * about and can reason with.
     */
    CmsExtensionContext getContext();

    /**
     * The URL path that loads the extension over the origin of the CMS.
     *
     * For example: if the CMS runs at https://localhost:8080/cms, and the URL path is "/site/_cmsinternal/myextension",
     * the CMS will load the extension via a GET request to https://localhost:8080/site/_cmsinternal/myextension.
     *
     * @return the URL path that loads the extension. Must start with a slash.
     */
    String getUrlPath();

}
