package org.hippoecm.hst.configuration.pagemapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import org.hippoecm.hst.configuration.pagemapping.component.AbstractJCRComponentService;
import org.hippoecm.hst.configuration.pagemapping.component.JCRComponentRootService;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.slf4j.LoggerFactory;

public class HstComponentsService  extends AbstractJCRService  implements HstComponents{

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstComponents.class);
    
    private Map<String, HstComponent> componentServices;
    private String pageMappingNodePath;
    
    public HstComponentsService(Node pageMappingNode) throws RepositoryException {
        super(pageMappingNode);
        this.pageMappingNodePath = pageMappingNode.getPath();
        this.componentServices = new HashMap<String, HstComponent>();
        AbstractJCRComponentService rootComponentService = new JCRComponentRootService(this,pageMappingNode);
        populate(pageMappingNode, rootComponentService);
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
                    try {
                        if(AugmentableHstComponent.class.isAssignableFrom(Class.forName(clazz))) {
                            Constructor<?> cons = Class.forName(clazz).getConstructor(new Class[]{HstComponents.class, Node.class});
                            componentService = (AbstractJCRComponentService)cons.newInstance(this,child);
                            
                            /*
                             * put the component service with the key as a relative path of the component wrt the hst:pagemappings node
                             * The childPath will always be longer then the ancestors pageMappingNodePath
                             */ 
                            String key = childPath.substring(pageMappingNodePath.length()+1);
                            componentServices.put(key, componentService);
                            log.debug("Added component service for key '{}'",key);
                            if(parentComponent != null) {
                                parentComponent.addHierarchicalChildComponent(componentService);
                                log.debug("Added component service '{}' to parent component ",key);
                            }
                        } else {
                            log.warn("Skipping hst:component for '{}' because the classname property ("+clazz+") is not a subclass of {}", childPath, HstComponent.class.getName());
                        }
                    } catch (NoClassDefFoundError e){
                        log.warn("NoClassDefFoundError: Skipping hst:component for '{}' because the class ('"+clazz+"') cannot be found", childPath);
                    } catch (ClassNotFoundException e) {
                        log.warn("ClassNotFoundException: Skipping hst:component for '{}' because the class ('"+clazz+"') cannot be found", childPath);
                    } catch (InstantiationException e) {
                        log.warn("InstantiationException: Skipping hst:component for '{}' because class '{}' cannot be instiated", childPath, clazz);
                    } catch (IllegalAccessException e) {
                        log.warn("IllegalAccessException: Skipping hst:component for '{}'", childPath, e);  
                    } catch (SecurityException e) {
                        log.warn("SecurityException: Skipping hst:component for '{}'", childPath, e); 
                    } catch (NoSuchMethodException e) {
                        log.warn("NoSuchMethodException: Skipping hst:component for '{}'", childPath, e);  
                    } catch (IllegalArgumentException e) {
                        log.warn("IllegalArgumentException: Skipping hst:component for '{}'", childPath, e);  
                    } catch (InvocationTargetException e) {
                        log.warn("InvocationTargetException: Skipping hst:component for '{}'", childPath, e);  
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
        
        buf.append("\n\n------ PageMappingService ------ \n\n");
        buf.append(this.toString()  + "\n");
        for(Iterator<Entry<String, HstComponent>> entries = componentServices.entrySet().iterator(); entries.hasNext();) {
            Entry<String, HstComponent> entry = entries.next();
            buf.append("\n\tComponent: '" + entry.getKey() +"'");
            buf.append("\n\t\t"+entry.getValue().getClass().getName());
            appendChildComponents(entry.getValue(), buf, "\n\t\t\t");
        }
        
        buf.append("\n\n------ End PageMappingService ------");
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
