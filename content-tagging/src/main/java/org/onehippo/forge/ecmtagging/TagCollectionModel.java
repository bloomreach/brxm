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
package org.onehippo.forge.ecmtagging;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Wraps a TagCollection
 *
 * @author Jeroen Tietema
 *
 */
public class TagCollectionModel extends LoadableDetachableModel<TagCollection> {
    private static Logger log = LoggerFactory.getLogger(TagCollectionModel.class);
    private static final long serialVersionUID = 1L;

    private JcrNodeModel document;
    private TagSuggestor suggestor;

    public TagCollectionModel(JcrNodeModel document, TagSuggestor suggestor) {
        this.document = document;
        this.suggestor = suggestor;
    }

    public JcrNodeModel getDocument() {
        return document;
    }

    @Override
    protected TagCollection load() {
        if (document == null || suggestor == null) {
            log.error("Document or suggestor was null, cannot collect tags");
            return null;
        }
        try {
            return suggestor.getTags(document);
        } catch (RepositoryException e) {
            log.error("Error getting tags: ", e);
        }
        return null;
    }

}
