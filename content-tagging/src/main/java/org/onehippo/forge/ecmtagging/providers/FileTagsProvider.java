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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.ecmtagging.Tag;
import org.onehippo.forge.ecmtagging.TagCollection;
import org.onehippo.forge.ecmtagging.TaggingNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This provider searches for files embeded in the document.
 * This can be (local) links or images for instance.
 * 
 * @author Jeroen Tietema
 *
 * @todo Make the location of the facetselect configurable.
 */
public class FileTagsProvider extends AbstractTagsProvider {
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(FileTagsProvider.class);

    public final static String SCORE = "score";

    private double score;

    public FileTagsProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);
        score = config.getDouble(SCORE, 1);
    }

    @Override
    public TagCollection getTags(JcrNodeModel nodeModel) throws RepositoryException {
        TagCollection tagCollection = new TagCollection();
        Node document = nodeModel.getNode();
        log.debug("Searching for tags.");
        for (NodeIterator ii = document.getNodes(); ii.hasNext();) {
            Node html = ii.nextNode();
            for (NodeIterator i = html.getNodes(); i.hasNext();) {
                Node file = i.nextNode();
                // links to internal documents are accomplished with a facetselect to the document
                if (file.isNodeType(HippoNodeType.NT_FACETSELECT)){
                    log.debug("Found subnode of type facetselect: {}", file.getName());
                    NodeIterator ni = file.getNodes();

                    while(ni.hasNext()){
                      Node subFile = ni.nextNode(); // only has one node (always?)

                      if (subFile.isNodeType(TaggingNodeType.NT_TAGGABLE) && subFile.hasProperty(TaggingNodeType.PROP_TAGS)){
                          for (Value v : subFile.getProperty(TaggingNodeType.PROP_TAGS).getValues()){
                              tagCollection.add(new Tag(v.getString().trim(), score));
                              log.debug("Added tag: {}", v.getString());
                          }
                      }
                    }
                }
            }
        }
        return tagCollection;
    }

}
