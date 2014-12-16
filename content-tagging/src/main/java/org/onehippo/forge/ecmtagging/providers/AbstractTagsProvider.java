/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.ecmtagging.providers;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.ecmtagging.TagCollection;
import org.onehippo.forge.ecmtagging.TaggingNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All tag providers should extend this base class.
 * It registers the provider so that the suggestor can find it
 * and it provides an interface with a convenience method.
 * 
 * @author Jeroen Tietema
 *
 */
public abstract class AbstractTagsProvider implements ITagsProvider, IPlugin {


    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(AbstractTagsProvider.class);

    public AbstractTagsProvider(IPluginContext context, IPluginConfig config) {
        String serviceName = config.getString(ITagsProvider.SERVICE_ID, ITagsProvider.SERVICE_DEFAULT);
        context.registerService(this, serviceName);
        log.debug("Registered under {}", serviceName);
    }

    abstract public TagCollection getTags(JcrNodeModel nodeModel) throws RepositoryException;

    /**
     * Get the tags from the given source document
     * 
     * @param nodeModel
     * @return
     * @throws RepositoryException
     */
    protected ArrayList<String> getCurrentTags(JcrNodeModel nodeModel) throws RepositoryException {
        ArrayList<String> tags = new ArrayList<String>();
        Node document = nodeModel.getNode();
        if (document.hasProperty(TaggingNodeType.PROP_TAGS)) {
            for (Value tagValue : document.getProperty(TaggingNodeType.PROP_TAGS).getValues()) {
                tags.add(tagValue.getString());
            }
        }
        return tags;
    }
    
    public void start() {
        // Do nothing
    }

    public void stop() {
        // Do nothing
    }
}
