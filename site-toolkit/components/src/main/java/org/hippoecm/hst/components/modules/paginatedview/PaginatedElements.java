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
package org.hippoecm.hst.components.modules.paginatedview;


import java.util.List;

import javax.jcr.Node;

/**
 * The base interface that is used by the {@PaginateModule} to get the content
 * to display on a page. 
 */
public interface PaginatedElements {
   /**
    * Returns the items from the list that need to be displayed on a page.
    * @param from the first item in the list to be displayed on the page
    * @param to the last item in the list to be displayed on the page
    * @return
    */
   public List<Node> getElements(int from, int to);
   
   /**
    * The number of elements in the list.
    * @return
    */
   public int totalItems();
}
