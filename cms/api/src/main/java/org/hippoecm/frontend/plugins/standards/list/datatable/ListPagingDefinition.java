/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standards.list.datatable;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class ListPagingDefinition implements IPagingDefinition {
    private static final long serialVersionUID = 1L;


    private int viewSize;
    private int pageSize;

    public ListPagingDefinition() {
        pageSize = 15;
        viewSize = 10;
    }

    public ListPagingDefinition(IPluginConfig config) {
        this.pageSize = config.getInt("list.page.size", 15);
        this.viewSize = config.getInt("list.view.size", 10);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setViewSize(int viewSize) {
        this.viewSize = viewSize;
    }

    public int getViewSize() {
        return viewSize;
    }

}
