/*
 *  Copyright 2022 Bloomreach (https://www.bloomreach.com)
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

package org.hippoecm.frontend.settings;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;

public final class ContentSecurityPolicy {

    private String[] connectSources = new String[]{};
    private String[] frameAncestors = new String[]{};
    private String[] frameSources = new String[]{};
    private String[] imageSources = new String[]{};
    private String[] scriptSources = new String[]{};
    private String[] styleSources = new String[]{};

    public ContentSecurityPolicy(final Node cspNode) throws RepositoryException {
        connectSources = JcrUtils.getMultipleStringProperty(cspNode, "connect-src", connectSources);
        frameAncestors = JcrUtils.getMultipleStringProperty(cspNode, "frame-ancestors", frameAncestors);
        frameSources = JcrUtils.getMultipleStringProperty(cspNode, "frame-src", frameSources);
        imageSources = JcrUtils.getMultipleStringProperty(cspNode, "img-src", imageSources);
        scriptSources = JcrUtils.getMultipleStringProperty(cspNode, "script-src", scriptSources);
        styleSources = JcrUtils.getMultipleStringProperty(cspNode, "style-src", styleSources);
    }

    public String[] getConnectSources() {
        return connectSources;
    }

    public String[] getFrameAncestors() {
        return frameAncestors;
    }

    public String[] getFrameSources() {
        return frameSources;
    }

    public String[] getImageSources() {
        return imageSources;
    }

    public String[] getScriptSources() {
        return scriptSources;
    }

    public String[] getStyleSources() {
        return styleSources;
    }
}
