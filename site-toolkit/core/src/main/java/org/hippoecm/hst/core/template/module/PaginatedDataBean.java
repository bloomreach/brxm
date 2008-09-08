package org.hippoecm.hst.core.template.module;

import java.util.List;

public class PaginatedDataBean {
    private List items;
    private int totalItems;
    private int pageSize;
    private int pageNo;
    
    public List getItems() {
        return items;
    }
    public void setItems(List items) {
        this.items = items;
    }
    
    public int getTotalItems() {
        return totalItems;
    }
    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    public int getPageNo() {
        return pageNo;
    }
    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }
}
   
