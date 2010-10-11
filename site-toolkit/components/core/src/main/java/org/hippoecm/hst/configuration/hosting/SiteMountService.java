package org.hippoecm.hst.configuration.hosting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.HstSiteRootNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.site.HstSiteService;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiteMountService implements SiteMount {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SiteMountService.class);
    
    private static final String DEFAULT_TYPE = "live";
    /**
     * The name of this sitemount. If it is the root, it is called hst:root
     */
    private String name;
    
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
     * the alias of this SiteMount. If there is no specific property defined, the nodename is used as alias. 
     */
    private String alias;

    /**
     * The primary type of this SiteMount. If not specified, we use {@link #DEFAULT_TYPE} as a value
     */
    private String type = DEFAULT_TYPE;
    
    /**
     * The list of types excluding the primary <code>type</code> this SiteMount also belongs to
     */
    private List<String> types;
    
    
    /**
     * When the SiteMount is preview, and this isVersionInPreviewHeader is true, the used HST version is set as a response header. 
     * Default this variable is true when it is not configured explicitly
     */
    private boolean versionInPreviewHeader;
    
    /**
     * If this site mount must use some custom other than the default pipeline, the name of the pipeline is contained by <code>namedPipeline</code>
     */
    private String namedPipeline;
    

    /**
     * The mountpath of this sitemount. Note that it can contain wildcards
     */
    private String mountPath;
    
    /**
     * The absolute canonical path of the content
     */
    private String contentPath;
    
    /**
     * The absolute canonical path of the content
     */
    private String canonicalContentPath;

    /**
     * The path where the mount is pointing to
     */
    private String mountPoint;
    
    /**
     * <code>true</code> (default) when this SiteMount is used as a site mount. False when used only as content mount point and possibly a namedPipeline
     */
    private boolean isSiteMount = true;
    
    /**
     * The homepage for this SiteMount. When the backing configuration does not contain a homepage, then, the homepage from the backing {@link VirtualHost} is 
     * taken (which still might be <code>null</code> though)
     */
    private String homepage;
    

    /**
     * The pagenotfound for this SiteMount. When the backing configuration does not contain a pagenotfound, then, the pagenotfound from the backing {@link VirtualHost} is 
     * taken (which still might be <code>null</code> though)
     */
    private String pageNotFound;

    /**
     * whether the context path should be in the url.
     */
    private boolean contextPathInUrl;
    
    /**
     * whether the port number should be in the url. Default true
     */
    private boolean showPort = true;
    
    /**
     * default port is 0, which means, the site mount is port agnostic
     */
    private int port;
    
    /**
     *  when this sitemount is only applicable for certain contextpath, this property for the contextpath tells which value it must have. It must start with a slash.
     */
    private String onlyForContextPath;
    
    private String scheme;
    
    private boolean secured;
    
    private Set<String> roles;
    
    private Set<String> users;

    /**
     * for embedded delegation of sites a sitemountpath needs to point to the delegated sitemount. This is only relevant for portal environment
     */
    private String embeddedSiteMountPath;
    
    private boolean sessionStateful;
     
    public SiteMountService(HstNode siteMount, SiteMount parent, VirtualHost virtualHost, HstManagerImpl hstManager, int port) throws ServiceException {
        this.virtualHost = virtualHost;
        this.parent = parent;
        this.port = port;
        this.name = siteMount.getValueProvider().getName();

        // default for when there is no alias property
        
        this.alias = name;
        
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_ALIAS)) {
            this.alias = siteMount.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_ALIAS);
        }
        
        if(parent == null) {
            mountPath = "";
        } else {
            mountPath = parent.getMountPath() + "/" + name;
        }
       
        // is the context path visible in the url
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_SHOWCONTEXTPATH)) {
            this.contextPathInUrl = siteMount.getValueProvider().getBoolean(HstNodeTypes.SITEMOUNT_PROPERTY_SHOWCONTEXTPATH);
        } else {
            if(parent != null) {
                this.contextPathInUrl = parent.isContextPathInUrl();
            } else {
                this.contextPathInUrl = virtualHost.isContextPathInUrl();
            }
        }
     
        // is the port number visible in the url
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_SHOWPORT)) {
            this.showPort = siteMount.getValueProvider().getBoolean(HstNodeTypes.SITEMOUNT_PROPERTY_SHOWPORT);
        } else {
            if(parent != null) {
                this.showPort = parent.isPortInUrl();
            }
        }
        
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_ONLYFORCONTEXTPATH)) {
            this.onlyForContextPath = siteMount.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_ONLYFORCONTEXTPATH);
        } else {
            if(parent != null) {
                this.onlyForContextPath = parent.onlyForContextPath();
            } 
        }
        
        if(onlyForContextPath != null && !"".equals(onlyForContextPath)) {
            if(onlyForContextPath.startsWith("/")) {
                // onlyForContextPath starts with a slash. If it contains another /, it is configured incorrectly
                if(onlyForContextPath.substring(1).contains("/")) {
                    log.warn("Incorrectly configured 'onlyForContextPath' : It must start with a '/' and is not allowed to contain any other '/' slashes. We set onlyForContextPath to null");
                    onlyForContextPath = null;
                }
            }else {
                log.warn("Incorrect configured 'onlyForContextPath': It must start with a '/' to be used, but it is '{}'. We set onlyForContextPath to null", onlyForContextPath);
                onlyForContextPath = null;
            }
        }
        
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_SCHEME)) {
            this.scheme = siteMount.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_SCHEME);
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
        
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE)) {
            this.homepage = siteMount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.homepage = parent.getHomePage();
            } else {
                this.homepage = virtualHost.getHomePage();
            }
        }
        
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND)) {
            this.pageNotFound = siteMount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.pageNotFound = parent.getPageNotFound();
            } else {
                this.pageNotFound = ((VirtualHostService)virtualHost).getPageNotFound();
            }
        }
        
        
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            this.versionInPreviewHeader = siteMount.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.versionInPreviewHeader = parent.isVersionInPreviewHeader();
            } else {
                this.versionInPreviewHeader = ((VirtualHostService)virtualHost).isVersionInPreviewHeader();
            }
        }
        
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_TYPE)) {
            this.type = siteMount.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_TYPE);
        } 
        
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_TYPES)) {
            String[] typesProperty = siteMount.getValueProvider().getStrings(HstNodeTypes.SITEMOUNT_PROPERTY_TYPES);
            this.types = Arrays.asList(typesProperty);
        } 
        
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_ISSITEMOUNT)) {
            this.isSiteMount = siteMount.getValueProvider().getBoolean(HstNodeTypes.SITEMOUNT_PROPERTY_ISSITEMOUNT);
        } else if(parent != null) {
            this.isSiteMount = parent.isSiteMount();
        }

        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_NAMEDPIPELINE)) {
            this.namedPipeline = siteMount.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_NAMEDPIPELINE);
        } else if(parent != null) {
            this.namedPipeline = parent.getNamedPipeline();
        }
        

        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_EMBEDDEDSITEMOUNTPATH)) {
            this.embeddedSiteMountPath = siteMount.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_EMBEDDEDSITEMOUNTPATH);
        } else if(parent != null) {
            this.embeddedSiteMountPath = parent.getEmbeddedSiteMountPath();
        }
        
        if(siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_MOUNTPATH)) {
            this.mountPoint = siteMount.getValueProvider().getString(HstNodeTypes.SITEMOUNT_PROPERTY_MOUNTPATH);
            // now, we need to create the HstSite object
            if(mountPoint == null || "".equals(mountPoint)){
                mountPoint = null;
            }
        } else if(parent != null) {
            this.mountPoint = ((SiteMountService)parent).mountPoint;
            if(mountPoint != null) {
                log.info("mountPoint for SiteMount '{}' is inherited from its parent SiteMount and is '{}'", getName() , mountPoint);
            }
        }
        
        if (siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_SECURED)) {
            this.secured = siteMount.getValueProvider().getBoolean(HstNodeTypes.SITEMOUNT_PROPERTY_SECURED);
        } else if (parent != null){
            this.secured = parent.isSecured();
        } 
        
        if (siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_ROLES)) {
            String [] rolesProp = siteMount.getValueProvider().getStrings(HstNodeTypes.SITEMOUNT_PROPERTY_ROLES);
            this.roles = new HashSet<String>();
            CollectionUtils.addAll(this.roles, rolesProp);
        } else if (parent != null){
            this.roles = new HashSet<String>(parent.getRoles());
        } else {
            this.roles = new HashSet<String>();
        }
        
        if (siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_USERS)) {
            String [] usersProp = siteMount.getValueProvider().getStrings(HstNodeTypes.SITEMOUNT_PROPERTY_USERS);
            this.users = new HashSet<String>();
            CollectionUtils.addAll(this.users, usersProp);
        } else if (parent != null){
            this.users = new HashSet<String>(parent.getUsers());
        } else {
            this.users = new HashSet<String>();
        }
        
        if (siteMount.getValueProvider().hasProperty(HstNodeTypes.SITEMOUNT_PROPERTY_SESSIONSTATEFUL)) {
            this.sessionStateful = siteMount.getValueProvider().getBoolean(HstNodeTypes.SITEMOUNT_PROPERTY_SESSIONSTATEFUL);
        } else if (parent != null){
            this.sessionStateful = parent.isSessionStateful();
        } 
        
        // We do recreate the HstSite object, even when inherited from parent, such that we do not share the same HstSite object. This might be
        // needed in the future though, for example for performance reasons
        if(mountPoint == null ){
            log.info("SiteMount '{}' at '{}' does have an empty mountPoint. This means the SiteMount is not using a HstSite and does not have a content path", getName(), siteMount.getValueProvider().getPath());
        } else if(!mountPoint.startsWith("/")) {
            throw new ServiceException("SiteMount at '"+siteMount.getValueProvider().getPath()+"' has an invalid mountPoint '"+mountPoint+"'. A mount point is absolute and must start with a '/'");
        } else if(!isSiteMount()){
            log.info("SiteMount '{}' at '{}' does contain a mountpoint, but is configured not to be a mount to a hstsite", getName(), siteMount.getValueProvider().getPath());
        } else {
             
            HstSiteRootNode hstSiteNodeForMount = hstManager.getHstSiteRootNodes().get(mountPoint);
            if(hstSiteNodeForMount == null) {
                throw new ServiceException("mountPoint '" + mountPoint
                        + "' does not point to a hst:site node for SiteMount '" + siteMount.getValueProvider().getPath()
                        + "'. Cannot create HstSite for SiteMount");
            }
            
            this.hstSite = new HstSiteService(hstSiteNodeForMount, this, hstManager);
            this.canonicalContentPath = hstSiteNodeForMount.getCanonicalcontentPath();
            this.contentPath = hstSiteNodeForMount.getContentPath();
            log.info("Succesfull initialized hstSite '{}' for site mount '{}'", hstSite.getName(), getName());
        }
        
        // check whether there are child SiteMounts now for this SiteMount
        
        for(HstNode childMount : siteMount.getNodes()) {
            if(HstNodeTypes.NODETYPE_HST_SITEMOUNT.equals(childMount.getNodeTypeName())) {
                SiteMountService childMountService = new SiteMountService(childMount, this, virtualHost, hstManager, port);
                SiteMountService prevValue = this.childSiteMountServices.put(childMountService.getName(), childMountService);
                if(prevValue != null) {
                    log.warn("Duplicate child mount with same name below '{}'. The first one is overwritten and ignored.", siteMount.getValueProvider().getPath());
                }
            }
        }
        
        // add this site mount to the maps in the VirtualHostsService
        ((VirtualHostsService)virtualHost.getVirtualHosts()).addSiteMount(this);
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

    public String getAlias() {
        return alias;
    }
    
    public String getMountPath() {
        return mountPath;
    }
    
    public String getContentPath() {
        return contentPath;
    }

    public String getCanonicalContentPath() {
        return canonicalContentPath;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    public boolean isSiteMount() {
        return isSiteMount;
    }

    
    public SiteMount getParent() {
        return parent;
    }

  
    public String getScheme() {
        return scheme;
    }

    public String getHomePage() {
        return homepage;
    }
    
    public String getPageNotFound() {
        return pageNotFound;
    }

    public VirtualHost getVirtualHost() {
        return virtualHost;
    }

    public boolean isContextPathInUrl() {
        return contextPathInUrl;
    }

    public int getPort() {
        return port;
    }

    public boolean isPortInUrl() {
        return showPort;
    }
    public String onlyForContextPath() {
        return onlyForContextPath;
    }

    public boolean isPreview() {
        return isOfType("preview");
    }

    public String getType() {
        return type;
    }
    
    public List<String> getTypes(){
        List<String> combined = new ArrayList<String>();
        // add the primary type  first
        combined.add(getType());
        
        if(types != null) {
            if(types.contains(getType())) {
                for(String extraType : types) {
                    if(extraType != null) {
                       if(extraType.equals(getType())) {
                           // already got it
                           continue;
                       } 
                       combined.add(extraType);
                    }
                }
            } else {
                combined.addAll(types);
            }
        }
        return Collections.unmodifiableList(combined);
    }
    
    public boolean isOfType(String type) {
        return getTypes().contains(type);
    }

    
    public boolean isVersionInPreviewHeader() {
        return versionInPreviewHeader;
    }

    public String getNamedPipeline(){
        return namedPipeline;
    }

    public HstSiteMapMatcher getHstSiteMapMatcher() {
        return getVirtualHost().getVirtualHosts().getHstManager().getSiteMapMatcher();
    }

    public String getEmbeddedSiteMountPath() {
        return embeddedSiteMountPath;
    }

    public boolean isSecured() {
        return secured;
    }
    
    public Set<String> getRoles() {
        return Collections.unmodifiableSet(this.roles);
    }
    
    public Set<String> getUsers() {
        return Collections.unmodifiableSet(this.users);
    }

    public boolean isSessionStateful() {
        return sessionStateful;
    }



    
}
