package org.hippoecm.repository;

/**
 * This class is used in the {@link FacetedNavigationEngine#view()} method to inform the engine
 * if there should be returned document hits, how many hits should be returned and what the offset it
 */

public class HitsRequested {
    /*
     * Wether results should be returned
     */
    private boolean resultRequested;
    
    /*
     * How many results should be returned. Default is 10. Large values imply slow responses 
     */
    
    private int limit = 10;
    
    /*
     * The offset of the results
     */
    
    private int offset = 0;
    
    public HitsRequested(){
        
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isResultRequested() {
        return resultRequested;
    }

    public void setResultRequested(boolean resultRequested) {
        this.resultRequested = resultRequested;
    }
    
    
}
