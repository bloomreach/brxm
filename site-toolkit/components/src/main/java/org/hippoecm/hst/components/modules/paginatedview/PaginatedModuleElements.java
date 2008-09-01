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

import javax.servlet.jsp.PageContext;


/**
 * Implementations of this interface can be passed to the {@see PaginateModule} to
 * return the content/items of a page.
 *
 */
public interface PaginatedModuleElements extends PaginatedElements {
	   /**
	    * Passes the pageContext to the instance. This may be useful to get or set
	    * properties.
	    * @param pageContext
	    */
	   public void setPageContext(PageContext pageContext);
	   
	   /**
	    * This method is called by the {@see PaginateModule} to generate the list with
	    * elements to display on a page. This method is executed after the
	    * {@see setPageContext()} method.
	    */
	   public void construct();
	}
