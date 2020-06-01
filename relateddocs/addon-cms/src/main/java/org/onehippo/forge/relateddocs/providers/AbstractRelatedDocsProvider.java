/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.relateddocs.providers;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.relateddocs.RelatedDocCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRelatedDocsProvider implements IRelatedDocsProvider, IPlugin {

    private static final Logger log = LoggerFactory.getLogger(AbstractRelatedDocsProvider.class);
    private static final long serialVersionUID = 1L;


    public AbstractRelatedDocsProvider(IPluginContext context, IPluginConfig config) {
        context.registerService(this, SERVICE);

        if (log.isDebugEnabled()) {
            log.debug("Registered under " + SERVICE);
        }
    }

    public abstract RelatedDocCollection getRelatedDocs(JcrNodeModel nodeModel) throws RepositoryException;

    public void start() {
        // Do nothing
    }

    public void stop() {
        // Do nothing
    }

}
