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
package org.hippoecm.frontend.plugins.standards.browse;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;

public class BrowserSearchResultModel extends Model<BrowserSearchResult> implements IObservable {

    private static final long serialVersionUID = 1L;

    private IObservationContext obContext;
    private IChangeListener listener;

    public BrowserSearchResultModel(BrowserSearchResult bsr) {
        super(bsr);
    }

    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        this.obContext = context;
    }

    public void startObservation() {
        getObject().addChangeListener(listener = new IChangeListener() {
            private static final long serialVersionUID = 1L;

            public void onChange() {
                obContext.notifyObservers(new EventCollection());
            }

        });
    }

    public void stopObservation() {
        getObject().removeChangeListener(listener);
        listener = null;
    }

}
