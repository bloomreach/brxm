package org.hippoecm.hst.configuration.components;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.configuration.components.AugmentableHstComponent;
import org.hippoecm.hst.configuration.components.HstComponent;
import org.hippoecm.hst.configuration.components.HstComponents;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.slf4j.LoggerFactory;

public class HstComponentsService  extends AbstractJCRService  implements HstComponents{

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponents.class);
    
    private Map<String, HstComponent> componentServices;
    private String componentsNodePath;
    
    public HstComponentsService(Node componentsNode) throws RepositoryException {
        super(componentsNode);
        this.componentsNodePath = componentsNode.getPath();
        this.componentServices = new HashMap<String, HstComponent>();
        HstComponentService rootComponentService = new HstComponentRootService(this,componentsNode);
        populate(componentsNode, rootComponentService);
    }
     
    public Service[] getChildServices() {
        return componentServices.values().toArray(new Service[componentServices.size()]);
    }

    
    public HstComponent getComponent(String path) {
        return this.componentServices.get(path);
    }
    
    private void populate(Node node, AugmentableHstComponent parentComponent) throws RepositoryException {
        
        for(NodeIterator nodeIt = node.getNodes(); nodeIt.hasNext();) {
            Node child = nodeIt.nextNode();
            if(child == null) {
                log.warn("skipping null node");
                continue;
            }
            AugmentableHstComponent componentService = null;
            if(child.isNodeType(Configuration.NODETYPE_HST_COMPONENT)) {
                if(child.hasProperty(Configuration.PROPERTYNAME_COMPONENT_CLASSNAME) && child.hasProperty(Configuration.PROPERTYNAME_JSP)) {
                    String childPath = child.getPath();
                    String clazz = child.getProperty(Configuration.PROPERTYNAME_COMPONENT_CLASSNAME).getString();
                    if("".equals(clazz)) {
                        log.debug("Skipping hst:component for '{}' because the classname property is empty", childPath);
                        continue;
                    }
                    
                    componentService = new HstComponentService(this, child);
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
                    log.debug("Skipping '{}' hst:component because it does not contain the mandatory properties '"+Configuration.PROPERTYNAME_COMPONENT_CLASSNAME+"' and '"+Configuration.PROPERTYNAME_JSP+"'.", child.getPath());
                }
            } else {
                log.warn("Skipping node '{}' because is not of type {}", child.getPath(), Configuration.NODETYPE_HST_COMPONENT);
            }
            
            /*
             * Populate hierarchical child components
             */
            populate(child, componentService);
            
        }
    }

    
    public void dump(StringBuffer buf, String indent) {
        
        buf.append("\n\n------ HstComponentsService ------ \n\n");
        buf.append(this.toString()  + "\n");
        for(Iterator<Entry<String, HstComponent>> entries = componentServices.entrySet().iterator(); entries.hasNext();) {
            Entry<String, HstComponent> entry = entries.next();
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
