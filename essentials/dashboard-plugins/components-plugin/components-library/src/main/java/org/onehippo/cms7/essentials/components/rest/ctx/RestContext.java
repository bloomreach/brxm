package org.onehippo.cms7.essentials.components.rest.ctx;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.essentials.components.rest.BaseRestResource;

/**
 * @version "$Id: RestContext.java 174715 2013-08-22 13:48:50Z mmilicevic $"
 */
public interface RestContext {

    boolean isMinimalDataSet();

    void setMinimalDataSet(boolean minimalDataSet);

    HttpServletRequest getRequest();

    HstRequestContext getRequestContext();

    int getResultLimit();

    void setResultLimit(int resultLimit);

    String getScope();

    boolean isAbsolutePath();

    void setAbsolutePath(boolean absolutePath);

    void setScope(String path);

    BaseRestResource getResource();

    HippoFolderBean getGalleryFolder();

    Map<String, String> getContextParams();
}
