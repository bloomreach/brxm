package org.hippoecm.hst.configuration.components;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.slf4j.LoggerFactory;

public class HstComponentService extends AbstractJCRService implements AugmentableHstComponent{
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponent.class);
    
    private String jsp;
    
    /**
     * String array of all the child components which are referenced by nodepath wrt hst:pagemappings in the hst:components multivalued property
     */
    private String[] childComponents;
    
    /**
     * List of ComponentService's that are a child component by hierarchy
     */
    private List<HstComponent> hierchicalChildComponents;
    
    /**
     * Array of ComponentService's that is composed of the String[] childComponents and List<ComponentService> hierchicalChildComponents
     */
    private HstComponent[] combinedComponents;

    private String namespace;
    
    private String name;

    private String componentSource;
    
    private String componentClassName;
    
    private HstComponents components;
    
    public HstComponentService(HstComponents components,Node jcrNode) {
        super(jcrNode);
        this.name = getValueProvider().getName();
        this.components = components;
        this.hierchicalChildComponents = new ArrayList<HstComponent>();
        if (getValueProvider().isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
            this.jsp = getValueProvider().getString(Configuration.PROPERTYNAME_JSP);
            this.namespace = getValueProvider().getString(Configuration.PROPERTYNAME_NAMESPACE);
            this.componentSource = getValueProvider().getString(Configuration.PROPERTYNAME_COMPONENTSOURCE);
            this.childComponents = getValueProvider().getStrings(Configuration.PROPERTYNAME_COMPONENTS);
            this.componentClassName = getValueProvider().getString(Configuration.PROPERTYNAME_COMPONENT_CLASSNAME);
        } 
    }
    
    public void addHierarchicalChildComponent(HstComponent hierarchicalChildComponent){
        this.hierchicalChildComponents.add(hierarchicalChildComponent);
        // reset the combined components such the getChilds returns the added component
        combinedComponents = null;
    }
    
    public HstComponent[] getChildComponents() {
        if(combinedComponents != null) {
            return combinedComponents;
        }
        if(childComponents == null && hierchicalChildComponents.isEmpty()) {
            return new HstComponent[0];
        } else {
            List<HstComponent> childs = new ArrayList<HstComponent>();
            if(childComponents != null) {
                for(String childComponent : this.childComponents) {
                    HstComponent c = this.components.getComponent(childComponent);
                    if(c!=null) {
                        childs.add(c);
                    } else {
                        log.warn("Cannot find a child component called '{}' for '{}'", childComponent, this);
                    }
                }
            }
            childs.addAll(hierchicalChildComponents);
            this.combinedComponents =  childs.toArray(new HstComponent[childs.size()]);
            return this.combinedComponents;
        }
    }

    
    public HstComponent[] getChildServices() {
        return this.getChildComponents();
    }

    
    public String getComponentClassName(){
        return this.componentClassName;
    }
    
    public String getJsp(){
        return this.jsp;
    }

   
    public String getNamespace() {
        return this.namespace;
    }

    public String getComponentSource(){
        return this.componentSource;
    }
    
    public HstComponents getHstComponents(){
        return this.components;
    }

    public String getName() {
        return this.name;
    }


    public void dump(StringBuffer buf, String indent) {
        buf.append(indent + getComponentClassName());
    }
    
}
