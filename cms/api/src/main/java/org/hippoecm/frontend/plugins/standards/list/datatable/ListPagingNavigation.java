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
package org.hippoecm.frontend.plugins.standards.list.datatable;

import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigation;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigationLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPagingLabelProvider;

public class ListPagingNavigation extends AjaxPagingNavigation {

    private static final long serialVersionUID = 1L;

    public ListPagingNavigation(final String id, final IPageable pageable, final IPagingLabelProvider labelProvider,
            IPagingDefinition pagingDefinition) {
        super(id, pageable, labelProvider);
        
        setViewSize(pagingDefinition.getViewSize());
    }
 
    @Override
    protected Link newPagingNavigationLink(String id, IPageable pageable, int pageIndex) {
        return new AjaxPagingNavigationLink(id, pageable, pageIndex);
    }
}
