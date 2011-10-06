package org.hippoecm.hst.behavioral.providers;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.behavioral.util.AnnotatedContentBeanClassesScanner;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHippoBeanDataProvider extends AbstractDataProvider {

    private static final Logger log = LoggerFactory.getLogger(AbstractHippoBeanDataProvider.class);

    private static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";
    
    private List<Class<? extends HippoBean>> annotatedClasses;
    private ObjectConverter objectConverter;

    public AbstractHippoBeanDataProvider(String id, String name, Node node) throws RepositoryException {
        super(id, name, node);
    }

    protected HippoBean getBeanForResolvedSiteMapItem(HstRequestContext requestContext) {
        ObjectConverter converter = getObjectConverter(requestContext);
        
        String base = PathUtils.normalizePath(requestContext.getResolvedMount().getMount().getContentPath());
        String relPath = PathUtils.normalizePath(requestContext.getResolvedSiteMapItem().getRelativeContentPath());
        if(relPath == null) {
            return null;
        }
        try {
            if("".equals(relPath)) {
                return (HippoBean) converter.getObject(requestContext.getSession(), "/"+base);
            } else {
                return (HippoBean) converter.getObject(requestContext.getSession(), "/"+base + "/" + relPath);
            }
        } catch (ObjectBeanManagerException e) {
            log.error("ObjectBeanManagerException. Return null : {}", e);
        } catch (RepositoryException e) {
            log.error("Could not get bean for path " + relPath, e);
        }
        return null;
        
    }
    
    private ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        if (objectConverter == null) {
            List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses(requestContext);
            objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }
        return objectConverter;
    }
    
    private List<Class<? extends HippoBean>> getAnnotatedClasses(HstRequestContext requestContext) {
        if (annotatedClasses == null) {
            annotatedClasses = AnnotatedContentBeanClassesScanner.scanAnnotatedContentBeanClasses(requestContext, requestContext.getServletContext().getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM));
        }
        return annotatedClasses;
    }

}
