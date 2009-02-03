package org.hippoecm.hst.configuration.pagemapping.components;

import org.hippoecm.hst.configuration.pagemapping.PageMapping;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.service.Service;

public interface Component extends Service{
    
    public void action(HstRequestContext hstRequestContext);
    
    public void doAction(HstRequestContext hstRequestContext);

    public void render(HstRequestContext hstRequestContext);
    
    public void doRender(HstRequestContext hstRequestContext);
    
    public String getJsp();
    
    public String getNamespace();
    
    public String getComponentSource();
    
    public String getComponentClassName();

    public PageMapping getPageMappingService();
 
}
