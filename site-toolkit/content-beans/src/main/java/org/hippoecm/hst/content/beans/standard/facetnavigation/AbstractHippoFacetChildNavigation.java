package org.hippoecm.hst.content.beans.standard.facetnavigation;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoFacetChildNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * The base class for child item of faceted navigation
 */
abstract public class AbstractHippoFacetChildNavigation extends HippoFolder implements HippoFacetChildNavigationBean {

    public Long getCount() {
        return this.getProperty(HippoNodeType.HIPPO_COUNT);
    }

    public HippoResultSetBean getResultSet() {
        return this.getBean(HippoNodeType.HIPPO_RESULTSET);
    }
    
    /**
     * AbstractHippoFacetChildNavigation does not return the HippoResultSetBean as a folder, as this is normally a 
     * folder that is not wanted to be displayed, and can be accessed through {@link #getResultSet()} already
     */
    @Override
    public List<HippoFolderBean> getFolders(boolean sorted){
        List<HippoFolderBean> folders = super.getFolders(sorted);
        
        List<HippoFolderBean> remove = new ArrayList<HippoFolderBean>();
        for(HippoFolderBean folder : folders) {
            if(folder instanceof HippoResultSetBean) {
                remove.add(folder);
            }
        }
        this.hippoFolders.removeAll(remove);
        return this.hippoFolders;
    }

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
