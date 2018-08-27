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
package org.hippoecm.frontend.plugins.standards.list;

import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.markup.html.IHeaderContributor;

public interface IListColumnProvider extends IClusterable {

    String SERVICE_ID = "column.id";

    IHeaderContributor getHeaderContributor();

    List<ListColumn<Node>> getColumns();

    default List<ListColumn<Node>> getTypeViewColumns() {
        return getColumns();
    }

    List<ListColumn<Node>> getExpandedColumns();

}
