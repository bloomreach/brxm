/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
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

    private final SystemInfoDataProvider memoryInfo = new SystemInfoDataProvider();

    public SystemInfoPanel(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);

        final List<ICellPopulator> columns = new ArrayList<>(2);
        columns.add(new PropertyPopulator("Key"));
        columns.add(new PropertyPopulator("Value"));
        add(new DataGridView("rows", columns, memoryInfo) {
            protected Item newRowItem(final String id, final int index, final IModel model) {
                return new OddEvenItem(id, index, model);
            }
        });
    }

    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("admin-system-info-title");
    }

}
