/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.list.generic.datatable;

import org.hippoecm.frontend.model.JcrNodeModel;

public interface ICustomizableDocumentListingDataTable {

    /**
     * Adds default top paging above document listing. Calling this method multiple times
     * will not duplicate the paging, so only calling it ones makes sense.
     *
     */
    public abstract void addTopPaging();

    /**
     * Adds default top paging above document listing. Calling this method multiple times
     * will not duplicate the paging, so only calling it ones makes sense.
     *
     */
    public abstract void addTopPaging(int viewSize);


    /**
     * Adds default ajax top headers above document listing. Calling this method multiple times
     * will not duplicate the paging, so only calling it ones makes sense.
     * @param int viewSize : the number of pages visible at one moment
     *
     */
    public abstract void addTopColumnHeaders();

    /**
     * Add default ajax bottom paging above document listing. Calling this method multiple times
     * will not duplicate the paging, so only calling it ones makes sense.
     *
     */
    public abstract void addBottomPaging();

    /**
     * Add default ajax bottom paging above document listing. Calling this method multiple times
     * will not duplicate the paging, so only calling it ones makes sense.
     * @param int viewSize : the number of pages visible at one moment
     *
     */
    public abstract void addBottomPaging(int viewSize);

    /**
     * Adds default ajax bottom headers above document listing. Calling this method multiple times
     * will not duplicate the paging, so only calling it ones makes sense.
     *
     */
    public abstract void addBottomColumnHeaders();
    
    /**
     * Selects the node/document to highlight in the list.
     * @param    nodeModel    A {@link JcrNodeModel} representing a node in the list 
     */
    public void setSelectedNode(JcrNodeModel nodeModel);

}
