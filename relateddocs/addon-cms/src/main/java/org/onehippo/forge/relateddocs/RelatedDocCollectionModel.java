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
package org.onehippo.forge.relateddocs;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelatedDocCollectionModel implements IModel<RelatedDocCollection> {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RelatedDocCollectionModel.class);

    private RelatedDocCollection collection;
    private JcrNodeModel document;
    private IPluginContext context;

    public RelatedDocCollectionModel(final JcrNodeModel document) {
        this.document = document;
    }

    public RelatedDocCollectionModel(RelatedDocCollection collection, JcrNodeModel document) {
        this.collection = collection;
        this.document = document;
    }

    public RelatedDocCollectionModel(RelatedDocCollection collection, JcrNodeModel document, IPluginContext context) {
        this.collection = collection;
        this.document = document;
        this.context = context;
    }

    public JcrNodeModel getDocument() {
        return document;
    }

    @Override
    public RelatedDocCollection getObject() {
        if (collection == null) {
            load();
        }
        return collection;
    }

    @Override
    public void setObject(final RelatedDocCollection object) {
        collection = object;
    }

    public void detach() {
        if (collection != null) {
            collection.detach();
        }
        collection = null;
        if (document != null) {
            document.detach();
        }
    }

    private void load() {
        if (context != null) {
            RelatedDocSuggestor suggester = context.getService(RelatedDocSuggestor.SERVICE, RelatedDocSuggestor.class);
            if (suggester != null) {
                collection = suggester.getRelatedDocCollection(document);
            }
        } else {
            collection = new RelatedDocCollection(document);
        }
    }

    public static RelatedDocCollectionModel from(final Node node) {
        return new RelatedDocCollectionModel(new JcrNodeModel(node));
    }
}
