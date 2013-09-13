package org.onehippo.cms7.essentials.components.rest.ctx;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.essentials.components.rest.BaseRestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: DefaultRestContext.java 174726 2013-08-22 14:24:50Z mmilicevic $"
 */
public class DefaultRestContext implements RestContext {

    public static final int RESULT_LIMIT = 10;
    private static final Logger log = LoggerFactory.getLogger(DefaultRestContext.class);
    private final HttpServletRequest request;
    private final HstRequestContext context;
    private final BaseRestResource resource;
    private Map<String, String> contextParams = new HashMap<String, String>();
    private int resultLimit;
    private boolean minimalDataSet;
    private boolean absolutePath;
    private String scope;


    public DefaultRestContext(BaseRestResource resource, HttpServletRequest request, HstRequestContext context) {
        this.request = request;
        this.context = context;
        this.resultLimit = RESULT_LIMIT;
        this.resource = resource;


    }

    public DefaultRestContext(BaseRestResource resource, HttpServletRequest request, HstRequestContext context, int resultLimit) {
        this(resource, request, context);
        this.resultLimit = resultLimit;
    }

    @Override
    public boolean isMinimalDataSet() {
        return minimalDataSet;
    }

    @Override
    public void setMinimalDataSet(boolean minimalDataSet) {
        this.minimalDataSet = minimalDataSet;
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public HstRequestContext getRequestContext() {
        return context;
    }

    @Override
    public int getResultLimit() {
        return resultLimit;
    }

    @Override
    public void setResultLimit(final int resultLimit) {
        this.resultLimit = resultLimit;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public void setScope(String scopePath) {
        this.scope = scopePath;
    }

    @Override
    public boolean isAbsolutePath() {
        return absolutePath;
    }

    @Override
    public void setAbsolutePath(final boolean absolutePath) {
        this.absolutePath = absolutePath;
    }

    @Override
    public Map<String, String> getContextParams() {
        return this.contextParams;
    }

    @Override
    public BaseRestResource getResource() {
        return resource;
    }

    public HippoFolderBean getGalleryFolder() {
        try {
            final ObjectConverter objectConverter = resource.getObjectConverter(context);
            HippoBean gallery = (HippoBean) objectConverter.getObject(context.getSession(), "/content/gallery");
            if (gallery instanceof HippoFolderBean) {
                return (HippoFolderBean) gallery;
            } else {
                log.warn("Gallery base folder not of type folder. Cannot return folder bean for it. Return null");
            }
        } catch (ObjectBeanManagerException e) {
            log.warn("Cannot find the root Gallery folder. Return null");
        } catch (RepositoryException e) {
            log.error("Error obtaining session", e);
        }

        return null;
    }
}
