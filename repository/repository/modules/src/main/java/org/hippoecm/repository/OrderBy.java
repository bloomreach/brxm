package org.hippoecm.repository;

public class OrderBy {
    private String name;
    private boolean descending;
    
    public OrderBy(String name) {
        this.name = name;
    }
            
    public OrderBy(String name, boolean descending) {
        this.name = name;
        this.descending = descending;
    }

    public String getName() {
        return name;
    }

    public boolean isDescending() {
        return descending;
    }
}
