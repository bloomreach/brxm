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
package org.hippoecm.frontend.widgets;

import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;

public abstract class AbstractView extends DataView {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public AbstractView(String wicketId, IDataProvider provider) {
        super(wicketId, provider);

        setItemReuseStrategy(new ManagedReuseStrategy() {
            private static final long serialVersionUID = 1L;

            @Override
            public void destroyItem(Item item) {
                AbstractView.this.destroyItem(item);
            }
        });
    }

    public void populate() {
        super.onPopulate();
    }

    protected void destroyItem(Item item) {
    }
}
