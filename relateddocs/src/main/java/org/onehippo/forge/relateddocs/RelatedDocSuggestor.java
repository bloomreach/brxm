/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.relateddocs;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.relateddocs.providers.IRelatedDocsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelatedDocSuggestor implements IClusterable, IPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RelatedDocSuggestor.class);

    /**
     * The servicename under which the calling resources can retrieve the RelatedDocsSuggester
     * <p/>
     * TODO: make this configurable in the repository
     */
    public static final String SERVICE = "relateddocs.suggestor";

    private IPluginContext context;

    public RelatedDocSuggestor(IPluginContext context, IPluginConfig config) {
        this.context = context;
        context.registerService(this, RelatedDocSuggestor.SERVICE);
    }

    /**
     * IRelatedDocsProvider implementation.
     *
     * @param nodeModel Model referencing a document node that contains related documents.
     * @return RelatedDocCollectionModel
     */
    public RelatedDocCollectionModel getRelatedDocs(JcrNodeModel nodeModel) {
        return new RelatedDocCollectionModel(getRelatedDocCollection(nodeModel), nodeModel, context);
    }

    /**
     * Return collection of related docs, all scores are normalised (top has 100%).
     *
     * @param nodeModel Model referencing a document node that contains related documents.
     * @return RelatedDocCollection containing all related documents.
     */
    public RelatedDocCollection getRelatedDocCollection(JcrNodeModel nodeModel) {
        List<IRelatedDocsProvider> providers =
                context.getServices(IRelatedDocsProvider.SERVICE, IRelatedDocsProvider.class);

        if (log.isDebugEnabled()) {
            log.debug("Loaded {} providers.", providers.size());
        }

        RelatedDocCollection allDocs = new RelatedDocCollection();
        for (IRelatedDocsProvider provider : providers) {
            RelatedDocCollection relatedDocs;
            try {
                relatedDocs = provider.getRelatedDocs(nodeModel);
                if (log.isDebugEnabled()) {
                    if (relatedDocs == null) {
                        log.debug("Got null as relatedDocs from {}", provider.getClass());
                    } else {
                        log.debug("Got {} relatedDocs from {}", relatedDocs.size(), provider.getClass());
                    }
                }
                if (relatedDocs != null) {
                    allDocs.addAll(relatedDocs);
                }
            } catch (RepositoryException e) {
                // one of the providers can throw and error but others may be handled normally
                log.error("Retrieving relatedDocs from " + provider.getClass().getName()
                        + " caused an error and will be skipped.", e);
            }
        }
        allDocs.normalizeScores();
        return allDocs;
    }

    public void start() {
        // Do nothing
    }

    public void stop() {
        // Do nothing
    }

}
