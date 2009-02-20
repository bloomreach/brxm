package org.hippoecm.hst.hstconfiguration.components.fragments.header;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.hstconfiguration.components.HstComponentBase;
import org.hippoecm.hst.pagetypes.PageStyleType;
import org.hippoecm.hst.service.ServiceFactory;

public class PageStyle extends HstComponentBase {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        
        super.doBeforeRender(request, response);
        
        HstRequestContext hrc = request.getRequestContext();
        
        String componentContentPath =  this.hstComponentConfigurationBean.getComponentContentBasePath();
        if(componentContentPath != null) {
            try {
                Session session = hrc.getSession();
                Node contentPath = (Node)session.getItem(hrc.getSiteMapItem().getHstSiteMap().getSite().getContentPath());
                Node componentContent = contentPath.getNode(componentContentPath);
                if(componentContent.hasNode(componentContent.getName())) {
                    Node n = componentContent.getNode(componentContent.getName());
                    PageStyleType stylePage = ServiceFactory.create(n, PageStyleType.class);
                    request.setAttribute("style", stylePage); 
                }
                
            } catch (LoginException e) {
                e.printStackTrace();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
    }


    
}
