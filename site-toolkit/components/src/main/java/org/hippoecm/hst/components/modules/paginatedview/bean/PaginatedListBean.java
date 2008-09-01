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
package org.hippoecm.hst.components.modules.paginatedview.bean;

import java.util.List;

/**
 * The bean that is used by the {@see PaginateModule} to expose the EL properties
 * used by a page to display the paginated data.
 *
 */
public class PaginatedListBean {
    private long total;
	private int pageSize;
	private int pageId;
	private int itemsInPage;
	private List items;
	private boolean hasPrevious;
	private boolean hasNext;

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageId() {
		return pageId;
	}

	public void setPageId(int pageId) {
		this.pageId = pageId;
	}

	public int getItemsInPage() {
		return itemsInPage;
	}

	public void setItemsInPage(int itemsInPage) {
		this.itemsInPage = itemsInPage;
	}

	public List getItems() {
		return items;
	}

	public void setItems(List items) {
		this.items = items;
	}
   
   
}
