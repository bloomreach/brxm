package org.hippoecm.hst.configuration.pagemapping.component;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.pagemapping.PageMapping;
import org.hippoecm.hst.configuration.pagemapping.components.AugmentableComponent;
import org.hippoecm.hst.configuration.pagemapping.components.Component;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.hst.service.AbstractJCRService;
import org.slf4j.LoggerFactory;

public abstract class AbstractJCRComponentService extends AbstractJCRService implements AugmentableComponent{
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Component.class);
    
    private String jsp;
    
    /**
     * String array of all the child components which are referenced by nodepath wrt hst:pagemappings in the hst:components multivalued property
     */
    private String[] childComponents;
    
    /**
     * List of ComponentService's that are a child component by hierarchy
     */
    private List<Component> hierchicalChildComponents;
    
    /**
     * Array of ComponentService's that is composed of the String[] childComponents and List<ComponentService> hierchicalChildComponents
     */
    private Component[] combinedComponents;

    private String namespace;

    private String componentSource;
    
    private String componentClassName;
    
    private PageMapping pageMappingService;
    
    private JCRValueProvider valueProvider; 
    
    public AbstractJCRComponentService(PageMapping pageMappingService,Node jcrNode) {
        super(jcrNode);
        this.valueProvider = new JCRValueProviderImpl(jcrNode);
        this.pageMappingService = pageMappingService;
        this.hierchicalChildComponents = new ArrayList<Component>();
        if (valueProvider.isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
            this.jsp = valueProvider.getString(Configuration.PROPERTYNAME_JSP);
            this.namespace = valueProvider.getString(Configuration.PROPERTYNAME_NAMESPACE);
            this.componentSource = valueProvider.getString(Configuration.PROPERTYNAME_COMPONENTSOURCE);
            this.childComponents = valueProvider.getStrings(Configuration.PROPERTYNAME_COMPONENTS);
            this.componentClassName = valueProvider.getString(Configuration.PROPERTYNAME_COMPONENT_CLASSNAME);
        } 
    }
    
    public Component[] getChildServices() {
        return this.getChildComponents();
    }

    
    final public void action(HstRequestContext hstRequestContext){
        for(Component childs : getChildComponents()) {
            childs.action(hstRequestContext);
        }
        this.doAction(hstRequestContext);
    }
    
    final public void render(HstRequestContext hstRequestContext){
        for(Component childs : getChildComponents()) {
            childs.render(hstRequestContext);
        }
        this.doRender(hstRequestContext);
    }
    
    public String getComponentClassName(){
        return this.componentClassName;
    }
    
    public String getJsp(){
        return this.jsp;
    }

    public void addHierarchicalChildComponent(Component hierarchicalChildComponent){
        this.hierchicalChildComponents.add(hierarchicalChildComponent);
        // reset the combined components such the getChilds returns the added component
        combinedComponents = null;
    }
    
    public Component[] getChildComponents() {
        if(combinedComponents != null) {
            return combinedComponents;
        }
        if(childComponents == null && hierchicalChildComponents.isEmpty()) {
            return new Component[0];
        } else {
            List<Component> childs = new ArrayList<Component>();
            if(childComponents != null) {
                for(String childComponent : this.childComponents) {
                    Component c = this.pageMappingService.getComponent(childComponent);
                    if(c!=null) {
                        childs.add(c);
                    } else {
                        log.warn("Cannot find a child component called '{}' for '{}'", childComponent, this);
                    }
                }
            }
            childs.addAll(hierchicalChildComponents);
            this.combinedComponents =  childs.toArray(new Component[childs.size()]);
            return this.combinedComponents;
        }
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getComponentSource(){
        return this.componentSource;
    }
    
    public PageMapping getPageMappingService(){
        return this.pageMappingService;
    }

}
