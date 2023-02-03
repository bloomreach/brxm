/*
 *  Copyright 2008-2023 Bloomreach
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

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a TagCollection
 */
public class TagCollectionModel extends LoadableDetachableModel<TagCollection> {
    private static final Logger log = LoggerFactory.getLogger(TagCollectionModel.class);

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
            log.error("Document or suggester was null, cannot collect tags");
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
