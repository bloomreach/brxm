package org.hippoecm.hst.core.template.module.paginatedbrowse;

import java.util.List;

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
