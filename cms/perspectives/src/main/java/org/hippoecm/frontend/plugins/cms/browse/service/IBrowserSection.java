/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.service;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITitleDecorator;

/**
 * Extension service to the navigator / browser.  Plugins that wish to contribute
 * to the navigator should register a service that implements this interface at
 * their "wicket.id" service name.
 */
public interface IBrowserSection extends IRenderService, ITitleDecorator {

    class Match {
        private int distance;

        public void setDistance(int distance) {
            this.distance = distance;
        }

        public int getDistance() {
            return distance;
        }
    }
    
    /**
     * Does the section contain the specified Node.
     * Returns null when the section does not contain the node; otherwise, the distance
     * should be equivalent to the number of steps to the root node in the section.
     */
    Match contains(IModel<Node> node);

    /**
     * Select the Node in the section.
     */
    void select(IModel<Node> node);

    /**
     * The collection of documents in the current selection.  This collection will
     * be shown in the document listing.
     */
    DocumentCollection getCollection();

}
