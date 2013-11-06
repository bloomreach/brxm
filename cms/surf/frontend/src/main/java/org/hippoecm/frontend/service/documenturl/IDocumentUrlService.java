/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service.documenturl;

import javax.jcr.Node;

import org.apache.wicket.util.io.IClusterable;

/**
 * Returns the external URL to a Hippo document node.
 */
public interface IDocumentUrlService extends IClusterable {

    public static final String DEFAULT_SERVICE_ID = "default.document.url.service";

    /**
     * @param documentNode the Hippo document node
     * @return the external URL to a Hippo document node, or null of no external URL could be created.
     */
    public String getUrl(Node documentNode);

}
