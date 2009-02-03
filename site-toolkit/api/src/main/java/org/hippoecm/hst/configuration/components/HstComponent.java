package org.hippoecm.hst.configuration.components;

import org.hippoecm.hst.service.Service;

public interface HstComponent extends Service{
    
//    public void action(HstRequestContext hstRequestContext);
//    
//    public void doAction(HstRequestContext hstRequestContext);
//
//    public void render(HstRequestContext hstRequestContext);
//    
//    public void doRender(HstRequestContext hstRequestContext);
    
    public String getName();
    
    public String getJsp();
    
    public String getNamespace();
    
    public String getComponentSource();
    
    public String getComponentClassName();

    public HstComponents getHstComponents();
 
}
