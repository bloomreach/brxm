package org.hippoecm.hst.configuration.components;


public interface AugmentableHstComponentConfiguration extends HstComponentConfiguration{
    
    public void addHierarchicalChildComponent(HstComponentConfiguration hierarchicalChildComponent);
    
}
