package org.hippoecm.hst.content.beans.standard.facetnavigation;

import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoFacetChildNavigationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base class for child item of faceted navigation
 */
abstract public class AbstractHippoFacetChildNavigation extends HippoFacetNavigation implements HippoFacetChildNavigationBean {
    
    private static Logger log = LoggerFactory.getLogger(AbstractHippoFacetChildNavigation.class);
  
  
    /**
     * The ancestors and self of this AbstractHippoFacetChildNavigation. Note that only the ancestors of the same bean type are returned. 
     * @return the ancestors (only ancestors of the same type as 'this') + self list of AbstractHippoFacetChildNavigation's
     */
    abstract List<? extends AbstractHippoFacetChildNavigation> getAncestorsAndSelf();
    
    /**
     * he ancestors and self of this AbstractHippoFacetChildNavigation. Note that only the ancestors of the same bean type are returned. 
     * @return the ancestor list of AbstractHippoFacetChildNavigation's or an empty list if no ancestors present
     */
    abstract List<? extends AbstractHippoFacetChildNavigation> getAncestors();
    
}
