package org.hippoecm.hst.configuration.components;

import org.hippoecm.hst.configuration.components.HstComponent;
import org.hippoecm.hst.configuration.components.HstComponents;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.service.Service;

public abstract class AbstractHstComponentWrapper implements HstComponent {

    private HstComponent delegatee;
    
    public AbstractHstComponentWrapper(HstComponent hstComponent) {
        this.delegatee = hstComponent;
    }
    
    
//  final public void action(HstRequestContext hstRequestContext){
//      for(HstComponent childs : getChildComponents()) {
//          childs.action(hstRequestContext);
//      }
//      this.doAction(hstRequestContext);
//  }
//  
//  final public void render(HstRequestContext hstRequestContext){
//      for(HstComponent childs : getChildComponents()) {
//          childs.render(hstRequestContext);
//      }
//      this.doRender(hstRequestContext);
//  }

    public String getComponentClassName() {
        return delegatee.getComponentClassName();
    }

    public String getComponentSource() {
        return delegatee.getComponentSource();
    }

    public HstComponents getHstComponents() {
        return delegatee.getHstComponents();
    }

    public String getJsp() {
        return delegatee.getJsp();
    }

    public String getNamespace() {
        return delegatee.getNamespace();
    }

    public void closeValueProvider(boolean closeChildServices) {
        delegatee.closeValueProvider(closeChildServices);
    }

    public void dump(StringBuffer buf, String indent) {
        delegatee.dump(buf, indent);
    }

    public Service[] getChildServices() {
        return delegatee.getChildServices();
    }

    public ValueProvider getValueProvider() {
         return delegatee.getValueProvider();
    }

}
