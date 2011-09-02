package org.hippoecm.hst.tag;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.utils.EncodingUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for tags that generate a link based on a hippobean.
 */
public abstract class HstSurfAndEditTagSupport extends TagSupport {

    private final static Logger log = LoggerFactory.getLogger(HstSurfAndEditTag.class);

    private static final long serialVersionUID = 1L;

    protected HippoBean hippoBean;

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException{
        if (this.hippoBean == null || this.hippoBean.getNode() == null || !(this.hippoBean.getNode() instanceof HippoNode)) {
            log.warn("Cannot create a surf & edit link for a bean that is null or has a jcr node that is null or not an instanceof HippoNode");
            return EVAL_PAGE;
        }

        HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
        HstRequest hstRequest = HstRequestUtils.getHstRequest(servletRequest);

        if (hstRequest == null) {
            log.warn("Cannot create a surf & edit link outside the hst request processing for '{}'", this.hippoBean.getPath());
            return EVAL_PAGE;
        }

        HstRequestContext hstRequestContext = HstRequestUtils.getHstRequestContext(servletRequest);

        if (!hstRequestContext.isPreview()) {
            log.debug("Skipping surf & edit link because not in preview.");
            return EVAL_PAGE;
        }

        HippoNode node = (HippoNode) this.hippoBean.getNode();
        String nodeId = null;
        String nodeLocation = null;
        try {
            Node editNode = (HippoNode) node.getCanonicalNode();
            if (editNode == null) {
                log.debug("Cannot create a 'surf and edit' link for a pure virtual jcr node: '{}'", node.getPath());
                return EVAL_PAGE;
            } else {
                Node rootNode = (Node) editNode.getAncestor(0);
                if (editNode.isSame(rootNode)) {
                    log.warn("Cannot create a 'surf and edit' link for a jcr root node.");
                }
                if (editNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITES)) {
                    log.warn("Cannot create a 'surf and edit' link for a jcr node of type '{}'.", HstNodeTypes.NODETYPE_HST_SITES);
                }
                if (editNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITE)) {
                    log.warn("Cannot create a 'surf and edit' link for a jcr node of type '{}'.", HstNodeTypes.NODETYPE_HST_SITE);
                }

                Node handleNode = getHandleNodeIfIsAncestor(editNode, rootNode);
                if (handleNode != null) {
                    // take the handle node as this is the one expected by the cms edit link:
                    editNode = handleNode;
                    log.debug("The nodepath for the edit link in cms is '{}'", editNode.getPath());
                } else {
                    // do nothing, most likely, editNode is a folder node.
                }
                nodeId = editNode.getIdentifier();
                nodeLocation = editNode.getPath();
                log.debug("The nodeId for the edit link in cms is '{}', the path is '{}'", nodeId, nodeLocation);

            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve the node path for the edit location", e);
            return EVAL_PAGE;
        }

        if (nodeLocation == null) {
            log.warn("Did not find a jcr node location for the bean to create a cms edit location with. ");
            return EVAL_PAGE;
        }

        String encodedPath = EncodingUtils.getEncodedPath(nodeLocation, hstRequest);
        String surfAndEditLink = "";
        String cmsBaseUrl = hstRequestContext.getContainerConfiguration().getString(ContainerConstants.CMS_LOCATION);
        if (cmsBaseUrl != null && !"".equals(cmsBaseUrl)) {
            if (cmsBaseUrl.endsWith("/")) {
                cmsBaseUrl = cmsBaseUrl.substring(0, cmsBaseUrl.length() - 1);
            }
            surfAndEditLink = cmsBaseUrl + "?path="+encodedPath;
        }

        doRender(hstRequest, nodeId, surfAndEditLink);

        /*cleanup*/
        hippoBean = null;

        return EVAL_PAGE;
    }

    protected abstract void doRender(final HstRequest hstRequest, final String nodeId, final String surfAndEditLink) throws JspException;

    /*
     * when a currentNode is of type hippo:handle, we return this node, else we check the parent, until we are at the jcr root node.
     * When we hit the jcr root node, we return null;
     */
    protected final Node getHandleNodeIfIsAncestor(Node currentNode, Node rootNode) throws RepositoryException{
        if(currentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            return currentNode;
        }
        if(currentNode.isSame(rootNode)) {
            return null;
        }
        return getHandleNodeIfIsAncestor(currentNode.getParent(), rootNode);
    }

    public HippoBean getHippobean(){
        return this.hippoBean;
    }

    public void setHippobean(HippoBean hippoBean) {
        this.hippoBean = hippoBean;
    }
}
