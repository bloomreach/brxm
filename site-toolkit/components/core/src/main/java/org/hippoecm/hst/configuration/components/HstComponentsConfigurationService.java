package org.hippoecm.hst.configuration.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;

public class HstComponentsConfigurationService extends AbstractJCRService implements HstComponentsConfiguration, Service{

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentsConfiguration.class);
    
    /*
     * rootComponentConfigurations are component configurations that are directly retrievable through getComponentConfiguration(String id),
     * in other words, every HstComponent that has a non null id.
     * 
     */
    private Map<String, HstComponentConfiguration> rootComponentConfigurations;
    
    /*
     * Components that are direct childs of the hst:components node. A child component only is a root component when it has a non null
     * id.
     */
    private List<HstComponentConfiguration> childComponents;
    
    private String componentsNodePath;
    
    public HstComponentsConfigurationService(Node componentsNode) throws RepositoryException {
        super(componentsNode);
        this.componentsNodePath = componentsNode.getPath();
        this.rootComponentConfigurations = new HashMap<String, HstComponentConfiguration>();
        this.childComponents = new ArrayList<HstComponentConfiguration>();
        init(componentsNode);
        
        for(HstComponentConfiguration child: childComponents) {
            populateRootComponentConfigurations(child);
        }
    }
     
    public Service[] getChildServices() {
        return childComponents.toArray(new Service[rootComponentConfigurations.size()]);
    }

    
    public HstComponentConfiguration getComponentConfiguration(String id) {
        return this.rootComponentConfigurations.get(id);
    }
    

    public Map<String, HstComponentConfiguration> getComponentConfigurations() {
        return this.rootComponentConfigurations;
    }

    private void populateRootComponentConfigurations(HstComponentConfiguration componentConfiguration){
        if(componentConfiguration.getId() != null) {
            rootComponentConfigurations.put(componentConfiguration.getId(), componentConfiguration);
        }
        for(Iterator<HstComponentConfiguration> childsIt = componentConfiguration.getChildren().values().iterator(); childsIt.hasNext();){
            populateRootComponentConfigurations(childsIt.next());
        }
    }
    
    private void init(Node node) throws RepositoryException {
        
        for(NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext();) {
            Node child = nodeIt.nextNode();
            if(child == null) {
                log.warn("skipping null node");
                continue;
            }
            if(child.isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
                if(child.hasProperty(Configuration.COMPONENT_PROPERTY_REFERECENCENAME)) {
                    try {
                        HstComponentConfiguration componentConfiguration = new HstComponentConfigurationService(child, componentsNodePath);
                        childComponents.add(componentConfiguration);
                        log.debug("Added component service with key '{}'",componentConfiguration.getId());
                    } catch (ServiceException e) {
                        log.warn("Skipping component '{}'", child.getPath(), e);
                    }
                    
                    
                } else {
                    log.warn("Skipping '{}' hst:component + child components because it does not contain the mandatory property '{}'",Configuration.COMPONENT_PROPERTY_REFERECENCENAME, child.getPath());
                }
            } else if(child.isNodeType(Configuration.NODETYPE_HST_COMPONENTGROUP)) {
                init(child);
            }
            else {
                log.warn("Skipping node '{}' because is not of type '{}'", child.getPath(), (Configuration.NODETYPE_HST_COMPONENT + " | " + Configuration.NODETYPE_HST_COMPONENTGROUP));
            }
        }
    }

}
