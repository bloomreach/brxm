package org.hippoecm.hst.behavioral.providers;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentTypeDataProvider extends AbstractHippoBeanDataProvider {

    private static final Logger log = LoggerFactory.getLogger(DocumentTypeDataProvider.class);
    
    public DocumentTypeDataProvider(String id, String name, Node node) throws RepositoryException {
        super(id, name, node);
    }

    @Override
    protected List<String> extractTerms(HttpServletRequest request) {
        HstRequestContext requestContext = (HstRequestContext)request.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        
        HippoBean bean = getBeanForResolvedSiteMapItem(requestContext);
        if(bean == null) {
            return null;
        }

        if (bean.getNode() != null) {
            try {
                List<String> terms = new ArrayList<String>(1);
                terms.add(bean.getNode().getPrimaryNodeType().getName());
                return terms;
            } catch (RepositoryException e) {
                log.error("Could not extract document type from bean " + bean.getPath(), e);
            }
        }
        return null;
    }

}
