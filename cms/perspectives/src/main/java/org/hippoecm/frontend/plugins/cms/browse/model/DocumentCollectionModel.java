/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.model;

import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.model.ObservableModel;
import org.hippoecm.frontend.model.event.EventCollection;

public class DocumentCollectionModel extends ObservableModel<DocumentCollection> implements IChangeListener {

    public DocumentCollectionModel(DocumentCollection collection) {
        super(collection);
    }

    @Override
    public void startObservation() {
        super.startObservation();
        DocumentCollection collection = getObject();
        if (collection != null) {
            collection.addListener(this);
        }
    }

    @Override
    public void stopObservation() {
        DocumentCollection collection = getObject();
        if (collection != null) {
            collection.removeListener(this);
        }
        super.stopObservation();
    }

    public void onChange() {
        notifyObservers(new EventCollection());
    }

}
