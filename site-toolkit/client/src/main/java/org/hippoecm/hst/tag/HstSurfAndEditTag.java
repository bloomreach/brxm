/*
 *  Copyright 2009 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.configuration.Configuration;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.hst.utils.EncodingUtils;
import org.hippoecm.hst.utils.PageContextPropertyUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract supporting class for Hst Link tags
 */

public class HstSurfAndEditTag extends TagSupport {
    

    private final static Logger log = LoggerFactory.getLogger(HstSurfAndEditTag.class);
    
    private static final long serialVersionUID = 1L;

    protected HippoBean hippoBean;
    
    protected String path;
    
    protected String var;
    
    protected String scope;
    
    protected boolean skipTag;
    
    protected Map<String, List<String>> parametersMap = new HashMap<String, List<String>>();
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
    
        if (var != null) {
            pageContext.removeAttribute(var, PageContext.PAGE_SCOPE);
        }
        
        return EVAL_BODY_INCLUDE;
    }
    
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException{
        if(skipTag) {
            return EVAL_PAGE;
        }
        if(this.hippoBean == null || this.hippoBean.getNode() == null || !(this.hippoBean.getNode() instanceof HippoNode)) {
            log.warn("Cannot create a surf & edit link for a bean that is null or has a jcr node that is null or not an instanceof HippoNode");
            return EVAL_PAGE;
        }
        
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        // if hst request/response is retrieved, then this servlet has been dispatched by hst component.
        
        HstRequest hstRequest = (HstRequest) request.getAttribute(ContainerConstants.HST_REQUEST);
        HstResponse hstResponse = (HstResponse) request.getAttribute(ContainerConstants.HST_RESPONSE);
        
        if (hstRequest == null && pageContext.getRequest() instanceof HstRequest) {
            hstRequest = (HstRequest) pageContext.getRequest();
        }

        if(hstRequest == null) {
            log.warn("Cannot create a surf & edit link outside the hst request processing for '{}'", this.hippoBean.getPath());
            return EVAL_PAGE;
        }
        
        if (hstResponse == null && pageContext.getResponse() instanceof HstResponse) {
            hstResponse = (HstResponse) pageContext.getResponse();
        }

        HstRequestContext hstRequestContext = hstRequest.getRequestContext();
        String previewRepositoryEntryPath = hstRequest.getRequestContext().getContainerConfiguration().getString(ContainerConstants.PREVIEW_REPOSITORY_ENTRY_PATH, "");
        if(previewRepositoryEntryPath == null) {
            log.warn("Cannot create a surf & edit link because preview repository entry path is not configured. Configure '{}' property in your hst-config.properties.", ContainerConstants.PREVIEW_REPOSITORY_ENTRY_PATH);
            return EVAL_PAGE;
        }
        String siteContentBasePath = hstRequestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSite().getContentPath();
        siteContentBasePath = PathUtils.normalizePath(siteContentBasePath);
        boolean isPreview = (siteContentBasePath.startsWith(previewRepositoryEntryPath) ? true : false);
        if(!isPreview) {
            log.debug("Skipping surf & edit link because not in preview.");
            return EVAL_PAGE;
        }
        
        // cmsBaseUrl is something like : http://localhost:8080
        String cmsBaseUrl = hstRequestContext.getContainerConfiguration().getString(ContainerConstants.CMS_LOCATION);
        if(cmsBaseUrl == null || "".equals(cmsBaseUrl)) {
            log.warn("Skipping surf & edit link because cms location property is not configured: Configure '{}' property in your hst-config.properties.", ContainerConstants.CMS_LOCATION);
            return EVAL_PAGE;
        }
        if(cmsBaseUrl.endsWith("/")) {
            cmsBaseUrl = cmsBaseUrl.substring(0, cmsBaseUrl.length() -1);
        }
        
        String surfAndEditImgSrc = hstRequestContext.getContainerConfiguration().getString(ContainerConstants.SURF_AND_EDIT_IMAGE_SRC);
        if(surfAndEditImgSrc == null || "".equals(surfAndEditImgSrc)) {
            log.debug("Surf & edit link will have no image because surf and edit image not configured: Configure '{}' property in your hst-config.properties.", ContainerConstants.SURF_AND_EDIT_IMAGE_SRC);
        } else {
            HstLink link = hstRequestContext.getHstLinkCreator().create(surfAndEditImgSrc, hstRequestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSite(), true);
            surfAndEditImgSrc = link.toUrlForm(hstRequest, hstResponse, false);
        }
        
        HippoNode node = (HippoNode)this.hippoBean.getNode();
        String nodeLocation = null;
        try {
            Node editNode = (HippoNode)node.getCanonicalNode();
            if( editNode == null) {
                log.debug("Cannot create a 'surf and edit' link for a pure virtual jcr node: '{}'", node.getPath());
                return EVAL_PAGE;
            }  else {
                Node rootNode = (Node)editNode.getAncestor(0);
                if (editNode.isSame(rootNode)) {
                    log.warn("Cannot create a 'surf and edit' link for a jcr root node.");
                } 
                if (editNode.isNodeType(Configuration.NODETYPE_HST_SITES)) {
                    log.warn("Cannot create a 'surf and edit' link for a jcr node of type '{}'.", Configuration.NODETYPE_HST_SITES);
                }
                if (editNode.isNodeType(Configuration.NODETYPE_HST_SITE)) {
                    log.warn("Cannot create a 'surf and edit' link for a jcr node of type '{}'.", Configuration.NODETYPE_HST_SITE);
                } 
                
                Node handleNode = getHandleNodeIfIsAncestor(editNode, rootNode);
                if(handleNode != null) {
                    // take the handle node as this is the one expected by the cms edit link:
                    editNode = handleNode;  
                    log.debug("The nodepath for the edit link in cms is '{}'", editNode.getPath());
                } else {
                    // do nothing, most likely, editNode is a folder node.
                }
                nodeLocation = editNode.getPath();
                log.debug("The nodepath for the edit link in cms is '{}'", nodeLocation);
                
            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve the node path for the edit location", e);
            return EVAL_PAGE;
        }
        
        if(nodeLocation == null) {
            log.warn("Did not find a jcr node location for the bean to create a cms edit location with. ");
            return EVAL_PAGE;
        }
        
        String encodedPath = EncodingUtils.getEncodedPath(nodeLocation, request);
        
        String button = "";
        if(surfAndEditImgSrc != null) {
            button = "<img src=\""+surfAndEditImgSrc+"\" alt=\"surf and edit\" />";
        } else {
            button = "[surf & edit]";
        }
        String surfAndEdit = "<a href=\""+cmsBaseUrl + "?path="+encodedPath+"\" class=\"surfandeditlink\">"+button+"</a>";
        
        if (var == null) {
            try {               
                JspWriter writer = pageContext.getOut();
                writer.print(surfAndEdit);
            } catch (IOException ioe) {
                throw new JspException(
                    "Portlet/ResourceURL-Tag Exception: cannot write to the output writer.");
            }
        } 
        else {
            int varScope = PageContext.PAGE_SCOPE;
            
            if (this.scope != null) {
                if ("request".equals(this.scope)) {
                    varScope = PageContext.REQUEST_SCOPE;
                } else if ("session".equals(this.scope)) {
                    varScope = PageContext.SESSION_SCOPE;
                } else if ("application".equals(this.scope)) {
                    varScope = PageContext.APPLICATION_SCOPE;
                }
            }
            
            pageContext.setAttribute(var, surfAndEdit, varScope);
        }
        
        /*cleanup*/
        parametersMap.clear();
        var = null;
        hippoBean = null;
        scope = null;
        
        return EVAL_PAGE;
    }
    
    /*
     * when a currentNode is of type hippo:handle, we return this node, else we check the parent, until we are at the jcr root node.
     * When we hit the jcr root node, we return null;
     */ 
    private Node getHandleNodeIfIsAncestor(Node currentNode, Node rootNode) throws RepositoryException{
        if(currentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            return currentNode;
        }
        if(currentNode.isSame(rootNode)) {
            return null;
        }
        return getHandleNodeIfIsAncestor(currentNode.getParent(), rootNode);
    }


    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#release()
     */
    @Override
    public void release(){
        super.release();        
    }
    
    /**
     * Returns the var property.
     * @return String
     */
    public String getVar() {
        return var;
    }
    
    public String getScope() {
        return scope;
    }
    
    public HippoBean getHippobean(){
        return this.hippoBean;
    }
     
    public void setHippobean(HippoBean hippoBean) {
        this.hippoBean = hippoBean;
    }
    
    public void setHippobeanByBeanPath(String beanPath) {
        this.hippoBean = (HippoBean) PageContextPropertyUtils.getProperty(pageContext, beanPath);
        if(this.hippoBean == null) {
            log.debug("No bean for '{}'. The tag will be skipped.", beanPath);
            skipTag = true;
        }
    }
    
    /**
     * Sets the var property.
     * @param var The var to set
     * @return void
     */
    public void setVar(String var) {
        this.var = var;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    
    /* -------------------------------------------------------------------*/
        
    /**
     * TagExtraInfo class for HstSurfAndEditTag.
     */
    public static class TEI extends TagExtraInfo {
        
        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = null;
            String var = tagData.getAttributeString("var");
            if (var != null) {
                vi = new VariableInfo[1];
                vi[0] =
                    new VariableInfo(var, "java.lang.String", true,
                                 VariableInfo.AT_BEGIN);
            }
            return vi;
        }

    }
}
