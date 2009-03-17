#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.hstconfiguration.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.sitemenu.SiteMenu;
import org.hippoecm.hst.sitemenu.SiteMenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeftMenu  extends GenericHstComponent{

    public static final Logger log = LoggerFactory.getLogger(LeftMenu.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

        super.doBeforeRender(request, response);

        SiteMenuImpl siteMenu = new SiteMenuImpl();
        
        HstRequestContext reqContext = request.getRequestContext();
        
        HstLinkCreator linkCreator = reqContext.getHstLinkCreator();
        HstSiteMap hstSiteMap = reqContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap();
        for(HstSiteMapItem item : hstSiteMap.getSiteMapItems()){
            if(item.isVisible()) {
                HstLink link = linkCreator.create(item);
                SiteMenuItem hstSiteMenuItem = new SiteMenuItemImpl(link, item.getValue());
                siteMenu.addSiteMenuItem(hstSiteMenuItem);
            }
        }
        
        request.setAttribute("menu", siteMenu);
    }

    public class SiteMenuImpl implements SiteMenu{

        private static final long serialVersionUID = 1L;
        
        private List<SiteMenuItem> siteMenuItems = new ArrayList<SiteMenuItem>();
        
        
        public String getName() {
            return "sitemenu impl";
        }

        public void addSiteMenuItem(SiteMenuItem hstSiteMenuItem) {
            this.siteMenuItems.add(hstSiteMenuItem);
        }
        
        public List<SiteMenuItem> getSiteMenuItems() {
            return this.siteMenuItems;
        }

        public boolean isSelected() {
            return false;
        }

    }

    
    public class SiteMenuItemImpl implements SiteMenuItem {

        private static final long serialVersionUID = 1L;
        
        private HstLink link;
        private String name;
        
        public SiteMenuItemImpl(HstLink link, String name){
            this.name = name;
            this.link = link;
        }
        
        public List<SiteMenuItem> getChildMenuItems() {
            // no childs
            return Collections.emptyList();
        }

        public HstLink getHstLink() {
            return link;
        }

        public String getName() {
            return name;
        }

        public boolean isSelected() {
            return false;
        }
        
    }
}
