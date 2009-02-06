package org.hippoecm.hst.configuration.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.slf4j.LoggerFactory;

public class HstComponentConfigurationService extends AbstractJCRService implements HstComponentConfiguration, Service{
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentConfiguration.class);
    
    private String renderPath;
    
    /**
     * String array of all the child components which are referenced by nodepath wrt hst:pagemappings in the hst:components multivalued property
     */
    private String[] referencedComponents;
    
    /**
     * List of ComponentService's that are a child component by hierarchy
     */
    private List<HstComponentConfigurationService> childComponentConfigurations;
    
    private String namespace;
    
    private String name;

    private String componentSource;
    
    private String componentClassName;

    private HstComponentsConfiguration components;
    
    private HstComponentConfiguration parentComponent;
    
    private Map<String, Object> allProperties;
    
    public HstComponentConfigurationService(HstComponentsConfigurationService components, HstComponentConfiguration parentComponent,Node jcrNode) {
        super(jcrNode);
        this.name = getValueProvider().getName();
        this.components = components;
        this.parentComponent = parentComponent;
        this.childComponentConfigurations = new ArrayList<HstComponentConfigurationService>();
        if (getValueProvider().isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
            this.renderPath = getValueProvider().getString(Configuration.PROPERTYNAME_RENDER_PATH);
            this.namespace = getValueProvider().getString(Configuration.PROPERTYNAME_NAMESPACE);
            this.componentSource = getValueProvider().getString(Configuration.PROPERTYNAME_COMPONENTSOURCE);
            this.referencedComponents = getValueProvider().getStrings(Configuration.PROPERTYNAME_COMPONENTS);
            this.componentClassName = getValueProvider().getString(Configuration.PROPERTYNAME_COMPONENT_CLASSNAME);
            this.allProperties = getValueProvider().getProperties();
        } 
    }

    public Service[] getChildServices() {
        return childComponentConfigurations.toArray(new HstComponentConfigurationService[childComponentConfigurations.size()]);
    } 

    public List<HstComponentConfiguration> getChildren() {
        // next step is to also return referenced components
        return Arrays.asList(childComponentConfigurations.toArray(new HstComponentConfiguration[childComponentConfigurations.size()]));
    }
    
    public String getComponentClassName(){
        return this.componentClassName;
    }
    
    public String getRenderPath(){
        return this.renderPath;
    }

   
    public String getNamespace() {
        return this.namespace;
    }
    
    public HstComponentConfiguration getParentComponent() {
        return this.parentComponent;
    }

    
    public HstComponentsConfiguration getHstComponents(){
        return this.components;
    }

    public String getName() {
        return this.name;
    }

    public Map<String, Object> getProperties() {
        return allProperties;
    }
    
    
    public void dump(StringBuffer buf, String indent) {
        buf.append(indent + getComponentClassName());
    }

    public String getAlias() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getComponentContentBasePath() {
        return this.componentSource;
    }

    public String getContextRelativePath() {
        return this.componentSource;
    }

}
