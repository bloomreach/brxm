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
package org.hippoecm.frontend.plugins.cms.admin.system;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.PropertyPopulator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.AdminPerspective;

public class SystemInfoPanel extends Panel {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    private static final long serialVersionUID = 1L;

    private SystemInfoDataProvider memoryInfo = new SystemInfoDataProvider();
    
    public SystemInfoPanel(final String id, final AdminPerspective parent) {
        super(id);
        
        ICellPopulator[] columns = new ICellPopulator[2];
        columns[0] = new PropertyPopulator("Key");
        columns[1] = new PropertyPopulator("Value");
        add(new DataGridView("rows", columns, memoryInfo) {
            private static final long serialVersionUID = 1L;
            protected Item newRowItem(String id, int index, IModel model)
            {
                return new OddEvenItem(id, index, model);
            }
        });

        add(new AjaxFallbackLink("refresh") {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                memoryInfo.refresh();
                parent.refresh();
            }
        });

        add(new AjaxFallbackLink("close") {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                parent.showConfigPanel();
            }
        });
    }

}
