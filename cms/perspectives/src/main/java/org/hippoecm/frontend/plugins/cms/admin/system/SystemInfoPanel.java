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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.PropertyPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.model.SystemInfoDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;

public class SystemInfoPanel extends AdminBreadCrumbPanel {

    private static final long serialVersionUID = 1L;

    private SystemInfoDataProvider memoryInfo = new SystemInfoDataProvider();

    public SystemInfoPanel(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
        
        ICellPopulator[] columns = new ICellPopulator[2];
        columns[0] = new PropertyPopulator("Key");
        columns[1] = new PropertyPopulator("Value");
        add(new DataGridView("rows", columns, memoryInfo) {
            private static final long serialVersionUID = 1L;

            protected Item newRowItem(String id, int index, IModel model) {
                return new OddEvenItem(id, index, model);
            }
        });

        add(new AjaxLink("refresh") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                memoryInfo.refresh();
                target.addComponent(SystemInfoPanel.this);
            }
        });
        
        add(new AjaxLink("gc") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                System.gc();
                memoryInfo.refresh();
                target.addComponent(SystemInfoPanel.this);
            }
        });
    }

    public IModel<String> getTitle(Component component) {
        return new ResourceModel("admin-system-info-title");
    }

}
