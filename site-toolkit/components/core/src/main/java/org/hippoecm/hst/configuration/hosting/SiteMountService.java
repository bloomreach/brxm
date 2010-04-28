package org.hippoecm.hst.configuration.hosting;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.site.HstSiteService;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiteMountService extends AbstractJCRService implements SiteMount, Service {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(VirtualHostService.class);
    
    
    /**
     * The name of this sitemount. If it is the root, it is called hst:root
     */
    private String name;
    
    /**
     * The pathInfoPrefix of this sitemount. Note that it can contain wildcards
     */
    private String pathInfoPrefix;
    
    /**
     * The virtual host of where this SiteMount belongs to
     */
    private VirtualHost virtualHost;

    /**
     * The parent of this sitemount or null when this sitemount is the root
     */
    private SiteMount parent;
    
    /**
     * the HstSite this SiteMount points to
     */
    private HstSite hstSite;
    
    /**
     * The child site mounts below this sitemount
     */
    private Map<String, SiteMountService> childSiteMountServices = new HashMap<String, SiteMountService>();


    /**
     * Whether this sitemount points to a preview. Default is false
     */
    private boolean preview;
    
    /**
     * If this site mount must use some custom other than the default pipeline, the name of the pipeline is contained by <code>namedPipeline</code>
     */
    private String namedPipeline;
    
    /**
     * The path where the mount is pointing to
     */
    private String mountPath;
    
    private boolean portVisible;
    private int portNumber;
    private boolean contextPathInUrl;
    private String scheme;

    
    public SiteMountService(Node siteMount, SiteMount parent, VirtualHost virtualHost) throws ServiceException {
        super(siteMount);
        this.virtualHost = virtualHost;
        this.parent = parent;
        
        this.name = getValueProvider().getName();
        
        if(parent == null) {
            pathInfoPrefix = "";
        } else {
            pathInfoPrefix = parent.getPathInfoPrefix() + "/" + name;
        }
        
        // the portnumber
        if(getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_PORT)) {
            this.portNumber = getValueProvider().getLong(HstNodeTypes.SITEMOUNT_PROPERTY_PORT).intValue();
        } else if(parent != null) {
            this.portNumber = parent.getPortNumber();
        } else {
            this.portNumber = virtualHost.getPortNumber();
        }
        
        // is the portnumber visible
        if(getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_SHOWPORT)) {
            this.portVisible = getValueProvider().getBoolean((HstNodeTypes.SITEMOUNT_PROPERTY_SHOWPORT));
        } else if(parent != null) {
            this.portVisible = parent.isPortVisible();
        } else {
            this.portVisible = virtualHost.isPortVisible();
        }
        
        // is the context path visible in the url
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_SHOWCONTEXTPATH)) {
            this.contextPathInUrl = this.getValueProvider().getBoolean(HstNodeTypes.SITEMOUNT_PROPERTY_SHOWCONTEXTPATH);
        } else {
            if(parent != null) {
                this.contextPathInUrl = parent.isContextPathInUrl();
            } else {
                this.contextPathInUrl = virtualHost.isContextPathInUrl();
            }
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_SCHEME)) {
            this.scheme = this.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_SCHEME);
            if(this.scheme == null || "".equals(this.scheme)) {
                this.scheme = VirtualHostsService.DEFAULT_SCHEME;
            }
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.scheme = parent.getScheme();
            } else {
                this.scheme = virtualHost.getScheme();
            }
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_ISPREVIEW)) {
            this.preview = this.getValueProvider().getBoolean(HstNodeTypes.SITEMOUNT_PROPERTY_ISPREVIEW);
        } else if(parent != null) {
            this.preview = parent.isPreview();
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_NAMEDPIPELINE)) {
            this.namedPipeline = this.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_NAMEDPIPELINE);
        } else if(parent != null) {
            this.namedPipeline = parent.getNamedPipeline();
        }
        
        if(this.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_MOUNTPATH)) {
            this.mountPath = this.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_MOUNTPATH);
            // now, we need to create the HstSite object
            if(mountPath == null || "".equals(mountPath)){
                mountPath = null;
            }
        } else if(parent != null) {
            this.mountPath = ((SiteMountService)parent).mountPath;
            if(mountPath != null) {
                log.info("Mountpath for SiteMount '{}' is inherited from its parent SiteMount and is '{}'", getName() , mountPath);
            }
        }
        
        
        // We do recreate the HstSite object, even when inherited from parent, such that we do not share the same HstSite object. This might be
        // needed in the future
        if(mountPath == null ){
            log.info("SiteMount '{}' at '{}' does have an empty mountPath and will thus be used to skip hst request processing", getName(), getValueProvider().getPath());
        } else if(!mountPath.startsWith("/")) {
            throw new ServiceException("SiteMount at '"+getValueProvider().getPath()+"' has an invalid mountPath '"+mountPath+"'. A mount path is absolute and must start with a '/'");
        } else {
            try {
                if (siteMount.getSession().itemExists(mountPath)) {
                    if (siteMount.getSession().getItem(mountPath).isNode() && ((Node) siteMount.getSession().getItem(mountPath)).isNodeType(HstNodeTypes.NODETYPE_HST_SITE)) {
                        Node hstSiteNode = (Node) siteMount.getSession().getItem(mountPath);
                        this.hstSite = new HstSiteService(hstSiteNode, this);
                        log.info("Succesfull initialized hstSite '{}' for site mount '{}'", hstSite.getName(), getName());
                    } else {
                        throw new ServiceException("Mountpath '" + mountPath
                                + "' does not point to a hst:site node for SiteMount '" + getValueProvider().getPath()
                                + "'. Cannot create HstSite for SiteMount");
                    }
                }
            } catch (RepositoryException e) {
                throw new ServiceException("Error during creating HstSite. Cannot add SiteMount for '"+getValueProvider().getPath()+"'", e);
            }
        }
        
        // check whether there are child SiteMounts now for this SiteMount
        try {
            NodeIterator childMounts = siteMount.getNodes();
            while (childMounts.hasNext()) {
                Node childMountNode = childMounts.nextNode();
                if (childMountNode == null) {
                    continue;
                }
                SiteMountService childMount = new SiteMountService(childMountNode, this, virtualHost);
                SiteMountService prevValue = this.childSiteMountServices.put(childMount.getName(), childMount);
                if(prevValue != null) {
                    log.warn("Duplicate child mount with same name below '{}'. The first one is overwritten and ignored.", siteMount.getPath());
                }
            }
        } catch (RepositoryException e) {
            throw new ServiceException("Error during initializing site mounts", e);
        }
    }
    
    public ResolvedSiteMapItem match(HttpServletRequest request) throws MatchException {
        
        return null;
    }


    public SiteMount getChildMount(String name) {
        return childSiteMountServices.get(name);
    }

    public HstSite getHstSite() {
        return hstSite;
    }

    public String getName() {
        return name;
    }


    public String getPathInfoPrefix() {
        return pathInfoPrefix;
    }

    
    public SiteMount getParent() {
        return parent;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public String getScheme() {
        return scheme;
    }


    public VirtualHost getVirtualHost() {
        return virtualHost;
    }

    public boolean isContextPathInUrl() {
        return contextPathInUrl;
    }

    public boolean isPortVisible() {
        return portVisible;
    }

    public boolean isPreview() {
        return preview;
    }

    public Service[] getChildServices() {
        // the services are the child mounts AND the hstSite if this one is not null
        Service[] childServices = childSiteMountServices.values().toArray(new Service[childSiteMountServices.values().size()]);
        if(this.hstSite != null) {
            Service[] servicesPlusHstSite = new Service[childServices.length + 1];
            System.arraycopy(childServices, 0, servicesPlusHstSite, 0, childServices.length);
            // and add to the end the hstSite
            servicesPlusHstSite[childServices.length] = (Service)hstSite;
            return servicesPlusHstSite;
        } 
        return childServices;
    }
    
    public String getNamedPipeline(){
        return namedPipeline;
    }

    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return getVirtualHost().getVirtualHosts().getVirtualHostsManager().getHstSiteMapMatcher();
    }

}
