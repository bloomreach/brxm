package org.hippoecm.hst.configuration.hosting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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

public class MountService implements Mount {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MountService.class);
    
    
    private static final String DEFAULT_TYPE = "live";
    /**
     * The name of this {@link Mount}. If it is the root, it is called hst:root
     */
    private String name;
    
    /**
     * The virtual host of where this {@link Mount} belongs to
     */
    private VirtualHost virtualHost;

    /**
     * The parent of this {@link Mount} or null when this {@link Mount} is the root
     */
    private Mount parent;
    
    /**
     * the HstSite this {@link Mount} points to. It can be <code>null</code>
     */
    private HstSite hstSite;
    
    /**
     * The child {@link Mount} below this {@link Mount}
     */
    private Map<String, MountService> childMountServices = new HashMap<String, MountService>();

    /**
     * the alias of this {@link Mount}. <code>null</code> if there is no alias property
     */
    private String alias;
    
    private Map<String, Object> allProperties;

    /**
     * The primary type of this {@link Mount}. If not specified, we use {@link #DEFAULT_TYPE} as a value
     */
    private String type = DEFAULT_TYPE;
    
    /**
     * The list of types excluding the primary <code>type</code> this {@link Mount} also belongs to
     */
    private List<String> types;
    
    
    /**
     * When the {@link Mount} is preview, and this isVersionInPreviewHeader is true, the used HST version is set as a response header. 
     * Default this variable is true when it is not configured explicitly
     */
    private boolean versionInPreviewHeader;
    
    /**
     * If this {@link Mount} must use some custom other than the default pipeline, the name of the pipeline is contained by <code>namedPipeline</code>
     */
    private String namedPipeline;
    

    /**
     * The mountpath of this {@link Mount}. Note that it can contain wildcards
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
     * The path where the {@link Mount} is pointing to
     */
    private String mountPoint;
    
    /**
     * <code>true</code> (default) when this {@link Mount} is used as a site. False when used only as content mount point and possibly a namedPipeline
     */
    private boolean isMapped = true;
    
    /**
     * The homepage for this {@link Mount}. When the backing configuration does not contain a homepage, then, the homepage from the backing {@link VirtualHost} is 
     * taken (which still might be <code>null</code> though)
     */
    private String homepage;
    

    /**
     * The pagenotfound for this {@link Mount}. When the backing configuration does not contain a pagenotfound, then, the pagenotfound from the backing {@link VirtualHost} is 
     * taken (which still might be <code>null</code> though)
     */
    private String pageNotFound;

    /**
     * whether the context path should be in the url.
     */
    private boolean contextPathInUrl;
    
    // by default, isSite = true
    private boolean isSite = true;
    
    /**
     * whether the port number should be in the url. Default true
     */
    private boolean showPort = true;
    
    /**
     * default port is 0, which means, the {@link Mount} is port agnostic
     */
    private int port;
    
    /**
     *  when this {@link Mount} is only applicable for certain contextpath, this property for the contextpath tells which value it must have. It must start with a slash.
     */
    private String onlyForContextPath;

    private String scheme;
    
    /**
     * The locale for this {@link Mount}. When the backing configuration does not contain a locale, the value from a parent {@link Mount} is used. If there is
     * no parent, the value will be {@link VirtualHosts#getLocale()}. The locale can be <code>null</code>
     */
    private String locale;
    
    private boolean secured;
    
    private Set<String> roles;
    
    private Set<String> users;

    /**
     * for embedded delegation of sites a mountpath needs to point to the delegated {@link Mount}. This is only relevant for portal environment
     */
    private String embeddedMountPath;
    
    private boolean subjectBasedSession;
    
    private boolean sessionStateful;
    
    private String formLoginPage;
     
    public MountService(HstNode mount, Mount parent, VirtualHost virtualHost, HstManagerImpl hstManager, int port) throws ServiceException {
        this.virtualHost = virtualHost;
        this.parent = parent;
        this.port = port;
        this.name = mount.getValueProvider().getName();

        // default for when there is no alias property
        
        this.allProperties = mount.getValueProvider().getProperties();
      
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ALIAS)) {
            this.alias = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_ALIAS).toLowerCase();
        }
        
        if(parent == null) {
            mountPath = "";
        } else {
            mountPath = parent.getMountPath() + "/" + name;
        }
       
        // is the context path visible in the url
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SHOWCONTEXTPATH)) {
            this.contextPathInUrl = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SHOWCONTEXTPATH);
        } else {
            if(parent != null) {
                this.contextPathInUrl = parent.isContextPathInUrl();
            } else {
                this.contextPathInUrl = virtualHost.isContextPathInUrl();
            }
        }
     
        // is the port number visible in the url
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SHOWPORT)) {
            this.showPort = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SHOWPORT);
        } else {
            if(parent != null) {
                this.showPort = parent.isPortInUrl();
            }
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ONLYFORCONTEXTPATH)) {
            this.onlyForContextPath = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_ONLYFORCONTEXTPATH);
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
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SCHEME)) {
            this.scheme = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_SCHEME);
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
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE)) {
            this.homepage = mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.homepage = parent.getHomePage();
            } else {
                this.homepage = virtualHost.getHomePage();
            }
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE)) {
            this.locale = mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCALE);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.locale = parent.getLocale();
            } else {
                this.locale = virtualHost.getLocale();
            }
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND)) {
            this.pageNotFound = mount.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.pageNotFound = parent.getPageNotFound();
            } else {
                this.pageNotFound = ((VirtualHostService)virtualHost).getPageNotFound();
            }
        }
        
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            this.versionInPreviewHeader = mount.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        } else {
           // try to get the one from the parent
            if(parent != null) {
                this.versionInPreviewHeader = parent.isVersionInPreviewHeader();
            } else {
                this.versionInPreviewHeader = ((VirtualHostService)virtualHost).isVersionInPreviewHeader();
            }
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_TYPE)) {
            this.type = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_TYPE);
        } 
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_TYPES)) {
            String[] typesProperty = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_TYPES);
            this.types = Arrays.asList(typesProperty);
        } 
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ISMAPPED)) {
            this.isMapped = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_ISMAPPED);
        } else if(parent != null) {
            this.isMapped = parent.isMapped();
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_IS_SITE)) {
            this.isSite = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_IS_SITE);
        } else if(parent != null) {
            this.isSite = parent.isSite();
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_NAMEDPIPELINE)) {
            this.namedPipeline = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_NAMEDPIPELINE);
        } else if(parent != null) {
            this.namedPipeline = parent.getNamedPipeline();
        }
        

        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_EMBEDDEDMOUNTPATH)) {
            this.embeddedMountPath = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_EMBEDDEDMOUNTPATH);
        } else if(parent != null) {
            this.embeddedMountPath = parent.getEmbeddedMountPath();
        }
        
        if(mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
            this.mountPoint = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT);
            // now, we need to create the HstSite object
            if(mountPoint == null || "".equals(mountPoint)){
                mountPoint = null;
            }
        } else if(parent != null) {
            this.mountPoint = ((MountService)parent).mountPoint;
            if(mountPoint != null) {
                log.info("mountPoint for Mount '{}' is inherited from its parent Mount and is '{}'", getName() , mountPoint);
            }
        }
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SECURED)) {
            this.secured = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SECURED);
        } else if (parent != null){
            this.secured = parent.isSecured();
        } 
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_ROLES)) {
            String [] rolesProp = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_ROLES);
            this.roles = new HashSet<String>();
            CollectionUtils.addAll(this.roles, rolesProp);
        } else if (parent != null){
            this.roles = new HashSet<String>(parent.getRoles());
        } else {
            this.roles = new HashSet<String>();
        }
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_USERS)) {
            String [] usersProp = mount.getValueProvider().getStrings(HstNodeTypes.MOUNT_PROPERTY_USERS);
            this.users = new HashSet<String>();
            CollectionUtils.addAll(this.users, usersProp);
        } else if (parent != null){
            this.users = new HashSet<String>(parent.getUsers());
        } else {
            this.users = new HashSet<String>();
        }
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SUBJECTBASEDSESSION)) {
            this.subjectBasedSession = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SUBJECTBASEDSESSION);
        } else if (parent != null){
            this.subjectBasedSession = parent.isSubjectBasedSession();
        }
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_SESSIONSTATEFUL)) {
            this.sessionStateful = mount.getValueProvider().getBoolean(HstNodeTypes.MOUNT_PROPERTY_SESSIONSTATEFUL);
        } else if (parent != null){
            this.sessionStateful = parent.isSessionStateful();
        }
        
        if (mount.getValueProvider().hasProperty(HstNodeTypes.MOUNT_PROPERTY_FORMLOGINPAGE)) {
            this.formLoginPage = mount.getValueProvider().getString(HstNodeTypes.MOUNT_PROPERTY_FORMLOGINPAGE);
        } else if (parent != null){
            this.formLoginPage = parent.getFormLoginPage();
        }
        
        // We do recreate the HstSite object, even when inherited from parent, such that we do not share the same HstSite object. This might be
        // needed in the future though, for example for performance reasons
        if(mountPoint == null ){
            log.info("Mount '{}' at '{}' does have an empty mountPoint. This means the Mount is not using a HstSite and does not have a content path", getName(), mount.getValueProvider().getPath());
        } else if(!mountPoint.startsWith("/")) {
            throw new ServiceException("Mount at '"+mount.getValueProvider().getPath()+"' has an invalid mountPoint '"+mountPoint+"'. A mount point is absolute and must start with a '/'");
        } else if(!isMapped()){
            log.info("Mount '{}' at '{}' does contain a mountpoint, but is configured not to be a mount to a hstsite", getName(), mount.getValueProvider().getPath());
            // for non Mounts, the contentPath is just the mountpoint
            this.contentPath = mountPoint;
            // TODO HSTTWO- : the canonicalContentPath should be the canonical version of the contentPath in case it points to a virtual node.
            // this should be done when the HstConfigModel is in place
            this.canonicalContentPath = contentPath;
        } else {
             
            HstSiteRootNode hstSiteNodeForMount = hstManager.getHstSiteRootNodes().get(mountPoint);
            if(hstSiteNodeForMount == null) {
                throw new ServiceException("mountPoint '" + mountPoint
                        + "' does not point to a hst:site node for Mount '" + mount.getValueProvider().getPath()
                        + "'. Cannot create HstSite for Mount. Either fix the mountpoint or add 'hst:issitemount=false'");
            }
            
            this.hstSite = new HstSiteService(hstSiteNodeForMount, this, hstManager);
            this.canonicalContentPath = hstSiteNodeForMount.getCanonicalContentPath();
            this.contentPath = hstSiteNodeForMount.getContentPath();
            log.info("Succesfull initialized hstSite '{}' for Mount '{}'", hstSite.getName(), getName());
        }
        
        // check whether there are child Mounts now for this Mount
        
        for(HstNode childMount : mount.getNodes()) {
            if(HstNodeTypes.NODETYPE_HST_MOUNT.equals(childMount.getNodeTypeName())) {
                MountService childMountService = new MountService(childMount, this, virtualHost, hstManager, port);
                MountService prevValue = this.childMountServices.put(childMountService.getName(), childMountService);
                if(prevValue != null) {
                    log.warn("Duplicate child mount with same name below '{}'. The first one is overwritten and ignored.", mount.getValueProvider().getPath());
                }
            }
        }
        
        // add this Mount to the maps in the VirtualHostsService
        ((VirtualHostsService)virtualHost.getVirtualHosts()).addMount(this);
    }
    

    public Mount getChildMount(String name) {
        return childMountServices.get(name);
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

    public boolean isMapped() {
        return isMapped;
    }

    
    public Mount getParent() {
        return parent;
    }

  
    public String getScheme() {
        return scheme;
    }

    public String getLocale() {
        return locale;
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
    
    public boolean isSite() {
        return isSite;
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

    public String getEmbeddedMountPath() {
        return embeddedMountPath;
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
    
    public boolean isSubjectBasedSession() {
        return subjectBasedSession;
    }
    
    public boolean isSessionStateful() {
        return sessionStateful;
    }
    
    public String getFormLoginPage() {
        return formLoginPage;
    }
    
    public String getProperty(String name) {
        Object o = allProperties.get(name);
        if(o != null) {
            return o.toString();
        }
        return null;
    }
    
    public Map<String, String> getMountProperties() {
        Map<String, String> mountProperties = new HashMap<String, String>();
        for(Entry<String, Object> entry : allProperties.entrySet()) {
            if(entry.getValue() instanceof String) {
                if(entry.getKey().startsWith(PROPERTY_NAME_MOUNT_PREFIX)) {
                    if(entry.getKey().equals(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
                        // skip the hst:mountpoint property as this is a reserved property with a different meaning
                        continue;
                    }
                    mountProperties.put(entry.getKey().substring(PROPERTY_NAME_MOUNT_PREFIX.length()).toLowerCase(), ((String)entry.getValue()).toLowerCase());
                }
            }
        }
        return mountProperties;
    }


}
