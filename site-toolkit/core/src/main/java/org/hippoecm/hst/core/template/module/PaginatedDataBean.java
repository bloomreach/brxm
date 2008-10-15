package org.hippoecm.hst.core.template.module;

import java.util.List;
import java.util.Map;

import org.hippoecm.hst.core.template.node.el.ELPseudoMap;

public class PaginatedDataBean {
    private List items;
    private int totalItems;
    private int pageSize;
    private int pageNo;
    private String pageParameter;
    
    public String getPageParameter() {
        return pageParameter;
    }
    public void setPageParameter(String pageParameter) {
        this.pageParameter = pageParameter;
    }
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
    
    public boolean isHasPreviousPage() {
       if (pageNo > 0) {
          return true; 
       }
       return false;
    }
    
    public int getPreviousPageNo() {
        return pageNo == 0 ? 0 : pageNo - 1;
    }
    
    public int getNextPageNo() {
        return pageNo + 1;
    }
    
    public int getItemsInNextPage() {
        int remaining = totalItems - (pageNo * pageSize);
        return remaining > pageSize ? pageSize : remaining;
    }
    
    public boolean isHasNextPage() {
       int lastPage = getTotalPages();
       return pageNo < lastPage;
    }
    
    public int getTotalPages() {
       int lastPage = totalItems / pageSize;
       return (totalItems % pageSize == 0) ? lastPage : lastPage + 1; 
    }
    
    public Map getPreviousPageBlockPage() {
        return new ELPseudoMap() {            
            public Object get(Object bSize) {
                int blockSize = getCurrentPageBlockSize(bSize);

                 int currentPageBlock = pageNo / blockSize;
                 int previousPageBlock = currentPageBlock == 0 ? 0 : currentPageBlock - 1;
                 int previousPageBlockPage = previousPageBlock * blockSize;
                 return new Long(previousPageBlockPage);
            }
        };
    }
    
    public Map getNextPageBlockPage() {
        return new ELPseudoMap() {            
            public Object get(Object bSize) {
                int blockSize = getCurrentPageBlockSize(bSize);

                 int currentPageBlock = pageNo / blockSize;
                 int nextPageBlock = currentPageBlock + 1;
                 if (getTotalPages() < nextPageBlock * blockSize) {
                     nextPageBlock = currentPageBlock;
                 }                 
                 return new Long(nextPageBlock);
            }
        };
    }
    
    public Map getCurrentPageBlock() {
        return new ELPseudoMap() {
            public Object get(Object bSize) {
                int blockSize = getCurrentPageBlockSize(bSize);
                return new Long(pageNo / blockSize);
            }
        };
    }
    
    public Map getLastPageBlock() {
        return new ELPseudoMap() {            
            public Object get(Object bSize) {
                int blockSize = getCurrentPageBlockSize(bSize);
                int totalPages = getTotalPages();
                 
                 int fragment = (totalPages % blockSize > 0) && (totalPages >= blockSize) ? 1 : 0; 
                 int totalBlocks = (totalPages / blockSize) + fragment;
                 
                 //since we count the blockno's from 0: correct the last page block number
                 return new Long(totalBlocks == 0 ? 0 : totalBlocks - 1);
            }
        };
    }
    
    public Map getCurrentPageBlockStart() {
        return new ELPseudoMap() {            
            public Object get(Object bSize) {
                int blockSize = getCurrentPageBlockSize(bSize);
                 
                 int blockNo = pageNo / blockSize; 
                 return new Long (blockNo * blockSize);
            }
        };
    }
    
    public Map getCurrentPageBlockEnd() {
        return new ELPseudoMap() {            
            public Object get(Object bSize) {                
                 int blockSize = getCurrentPageBlockSize(bSize);
                 
                 int blockNo = pageNo / blockSize; 
                 int lastOfBlock = ((blockNo + 1) * blockSize) - 1;
                 int totalPages = getTotalPages() == 0 ? 0 : getTotalPages() - 1;
                 return new Long(totalPages < lastOfBlock ? totalPages : lastOfBlock);
            }
        };
    }
    
    protected int getCurrentPageBlockSize(Object bSize) {
        if (bSize instanceof String) {
            try {
                return Integer.parseInt((String) bSize);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            Long bLong = (Long) bSize;
            int blockSize = bLong.intValue();
            return new Long(pageNo / blockSize).intValue();
        }
   }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append("[");
        sb.append("totalItems=").append(totalItems);
        sb.append(", pageSize=").append(pageSize);
        sb.append(", pageNo=").append(pageNo);
        sb.append("]");
        return sb.toString();
    }
    
    
    
}
   
