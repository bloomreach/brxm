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

package org.hippoecm.frontend.plugins.yui.datatable;

import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.widget.WidgetBehavior;

public class DataTableBehavior extends WidgetBehavior {
    private static final long serialVersionUID = 1L;


    public DataTableBehavior() {
        this(new DataTableSettings());
    }

    public DataTableBehavior(DataTableSettings settings) {
        super(settings);

        getTemplate().setInstance("YAHOO.hippo.DataTable");
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "hippodatatable");
        super.addHeaderContribution(context);
    }

}
