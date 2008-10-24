/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.tagging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugins.tagging.providers.ITagsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagSuggestor implements Serializable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(TagSuggestor.class);

    private List<ITagsProvider> providers;
    private TagCollection tags;
    
    private static int instances = 0;

    public TagSuggestor(IPluginContext context) {
        instances++;
        providers = loadProviderCluster("cms-tagproviders", context);
        tags = new TagCollection();
    }
    
    public TagCollection getTags(JcrNodeModel nodeModel) {
        for (ITagsProvider provider : providers) {
            TagCollection tags = provider.getTags(nodeModel);
            this.tags.addAll(tags);
        }
        tags.normalizeScores();
        return tags;
    }
    
    private List<ITagsProvider> loadProviderCluster(String path, IPluginContext context){
        List<ITagsProvider> providers;
        // get cluster config via pluginconfigservice
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig cluster = pluginConfigService.getCluster(path);
        // check if cluster exists in repo
        if (cluster == null) {
            log.error("Unable to find provider cluster. Does it exist in repository?");
            providers = new ArrayList<ITagsProvider>();
        } else {
            String service = "providers_" + instances;
            cluster.put("tagging.service", service);
            context.start(cluster);
            // get all registered providers
            providers = context.getServices(service, ITagsProvider.class);
        }
        return providers;
    }

}
