package org.hippoecm.hst.configuration.components;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.components.AugmentableHstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.slf4j.LoggerFactory;

public class HstComponentsConfigurationService  extends AbstractJCRService  implements HstComponentsConfiguration{

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponentsConfiguration.class);
    
    private Map<String, HstComponentConfiguration> componentServices;
    private String componentsNodePath;
    
    public HstComponentsConfigurationService(Node componentsNode) throws RepositoryException {
        super(componentsNode);
        this.componentsNodePath = componentsNode.getPath();
        this.componentServices = new HashMap<String, HstComponentConfiguration>();
        HstComponentConfigurationService rootComponentService = new HstComponentConfigurationRootService(this,componentsNode);
        init(componentsNode, rootComponentService);
        
        // during populate, resolve all referenced components, and possibly indicated circular deps?
        // populate(rootComponentService);
    }
     
    public Service[] getChildServices() {
        return componentServices.values().toArray(new Service[componentServices.size()]);
    }

    
    public HstComponentConfiguration getComponent(String path) {
        return this.componentServices.get(path);
    }
    
    private void init(Node node, AugmentableHstComponentConfiguration parentComponent) throws RepositoryException {
        
        for(NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext();) {
            Node child = nodeIt.nextNode();
            if(child == null) {
                log.warn("skipping null node");
                continue;
            }
            AugmentableHstComponentConfiguration componentService = null;
            if(child.isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
                if(child.hasProperty(Configuration.PROPERTYNAME_COMPONENT_CLASSNAME) && child.hasProperty(Configuration.PROPERTYNAME_RENDER_PATH)) {
                    String childPath = child.getPath();
                    String clazz = child.getProperty(Configuration.PROPERTYNAME_COMPONENT_CLASSNAME).getString();
                    if("".equals(clazz)) {
                        log.debug("Skipping hst:component for '{}' because the classname property is empty", childPath);
                        continue;
                    }
                    
                    componentService = new HstComponentConfigurationService(this, parentComponent, child);
                    /*
                     * put the component service with the key as a relative path of the component wrt the hst:pagemappings node
                     * The childPath will always be longer then the ancestors pageMappingNodePath
                     */ 
                    String key = childPath.substring(componentsNodePath.length()+1);
                    componentServices.put(key, componentService);
                    log.debug("Added component service for key '{}'",key);
                    if(parentComponent != null) {
                        parentComponent.addHierarchicalChildComponent(componentService);
                        log.debug("Added component service '{}' to parent component ",key);
                    }

                } else {
                    log.debug("Skipping '{}' hst:component because it does not contain the mandatory properties '"+Configuration.PROPERTYNAME_COMPONENT_CLASSNAME+"' and '"+Configuration.PROPERTYNAME_RENDER_PATH+"'.", child.getPath());
                }
            } else {
                log.warn("Skipping node '{}' because is not of type {}", child.getPath(), Configuration.NODETYPE_HST_COMPONENT);
            }
            
            /*
             * Populate hierarchical child components
             */
            init(child, componentService);
            
        }
    }

    
    public void dump(StringBuffer buf, String indent) {
        
        buf.append("\n\n------ HstComponentsService ------ \n\n");
        buf.append(this.toString()  + "\n");
        for(Iterator<Entry<String, HstComponentConfiguration>> entries = componentServices.entrySet().iterator(); entries.hasNext();) {
            Entry<String, HstComponentConfiguration> entry = entries.next();
            buf.append("\n\tComponent: '" + entry.getKey() +"'");
            buf.append("\n\t\t"+entry.getValue().getComponentClassName());
            appendChildComponents(entry.getValue(), buf, "\n\t\t\t");
        }
        
        buf.append("\n\n------ End HstComponentsService ------");
    }


    private void appendChildComponents(Service componentService, StringBuffer buf, String indent) {
        Service[] components = componentService.getChildServices();
        if(components != null) {
            for(Service child : components) {
                buf.append(indent);
                buf.append(child.getClass().getName());
                appendChildComponents(child,buf,indent+"\t");
            }
        }
    }
    
}
