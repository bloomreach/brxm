package org.hippoecm.hst.configuration.components;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstComponentConfigurationService extends AbstractJCRService implements HstComponentConfiguration, Service{
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentConfiguration.class);
    
    private String renderPath;
    
    private SortedMap<String, HstComponentConfiguration> componentConfigurations = new TreeMap<String, HstComponentConfiguration>();
    
    private String id;

    private String componentContentBasePath;
    
    private String contextRelativePath;

    private String componentClassName;
    
    private String referenceName;
    
    private String referencedComponent;

    private Map<String, Object> allProperties;
    
    private String componentsRootNodePath;
    
    public HstComponentConfigurationService(Node jcrNode, String componentsRootNodePath) throws ServiceException {
        super(jcrNode);
        if(!getValueProvider().getPath().startsWith(componentsRootNodePath)) {
            throw new ServiceException("Node path of the component cannot start without the global components path. Skip Component");
        }
        this.componentsRootNodePath = componentsRootNodePath;
        // id is the relative path wrt configuration components path
        this.id = getValueProvider().getPath().substring(componentsRootNodePath.length()+1);
        if (getValueProvider().isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
            this.referenceName = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_REFERECENCENAME);
            this.renderPath = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_RENDER_PATH);
            this.componentContentBasePath = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_CONTENTBASEPATH);
            this.contextRelativePath = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_CONTEXTRELATIVEPATH);
            this.componentClassName = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_COMPONENT_CLASSNAME);
            this.referencedComponent = getValueProvider().getString(Configuration.COMPONENT_PROPERTY_REFERECENCECOMPONENT);
            this.allProperties = getValueProvider().getProperties();
        } 
        
        init(jcrNode);
       
    }

    public void init(Node jcrNode) {
        try {
            for(NodeIterator nodeIt = jcrNode.getNodes(); nodeIt.hasNext();) {
                Node child = nodeIt.nextNode();
                if(child == null) {
                    log.warn("skipping null node");
                    continue;
                }
                if(child.isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
                    if(child.hasProperty(Configuration.COMPONENT_PROPERTY_REFERECENCENAME)) {
                        try {
                            HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(child, componentsRootNodePath);
                            componentConfigurations.put(componentConfiguration.getId(), componentConfiguration);
                            log.debug("Added component service with key '{}'",componentConfiguration.getId());
                        } catch (ServiceException e) {
                            log.warn("Skipping component '{}'", child.getPath(), e);
                        }
                    } else {
                        log.debug("Skipping '{}' hst:component + child components because it does not contain the mandatory property '{}'",Configuration.COMPONENT_PROPERTY_REFERECENCENAME, child.getPath());
                    }
                } else {
                    log.warn("Skipping node '{}' because is not of type '{}'", child.getPath(), (Configuration.NODETYPE_HST_COMPONENT + " | " + Configuration.NODETYPE_HST_COMPONENTGROUP));
                }
            }
        } catch (RepositoryException e) {
            log.warn("Skipping Component due to Repository Exception ", e);
        }
        
    }
    
    public Service[] getChildServices() {
        return componentConfigurations.values().toArray(new Service[componentConfigurations.size()]);
    } 

    
    public String getComponentClassName(){
        return this.componentClassName;
    }
    
    public String getRenderPath(){
        return this.renderPath;
    }

    public Map<String, Object> getProperties() {
        return allProperties;
    }
    
    public String getComponentContentBasePath() {
        return this.componentContentBasePath;
    }

    public String getContextRelativePath() {
        return this.contextRelativePath;
    }

    public String getId() {
        return this.id;
    }

    public String getReferenceName() {
        return this.referenceName;
    }

    public SortedMap<String, HstComponentConfiguration> getChildren() {
       return this.componentConfigurations;
    }

}
