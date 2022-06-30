/*
 * Copyright 2008-2022 Bloomreach
 */
package org.hippoecm.hst.content.beans.manager;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.content.annotations.PageModelIgnoreType;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * Convert any kind of beans into JCR nodes {@literal &} properties.
 * <P>
 * This interface mimics Jackrabbit's one, but this is provided
 * to support more lightweight beans in HST.
 * </P>
 *
 * @version $Id$
 */
@PageModelIgnoreType
public interface ObjectConverter
{
	/**
	 * Returns the Primary Node Type for a Node (or a child Node if the Node is a Handle) to be used for Object conversion, 
	 * possibly using a fallback Node Type if no exact match can be determined.
	 * 
	 * @param node to determine the Primary Object Type for
	 * @return Primary Object Type to be used for Object conversion
	 * @throws ObjectBeanManagerException
	 */
    String getPrimaryObjectType(Node node) throws ObjectBeanManagerException;
    
    Object getObject(Session session, String path) throws ObjectBeanManagerException;
    
    Object getObject(Node node) throws ObjectBeanManagerException;

    Object getObject(Node node, String relPath) throws ObjectBeanManagerException;

    Object getObject(String uuid, Session session) throws ObjectBeanManagerException;
    
    Object getObject(String uuid, Node node) throws ObjectBeanManagerException;
    
    /**
     * @param jcrPrimaryNodeType Primary node type
     * @return the annotated <code>Class</code> for this jcrPrimaryNodeType or <code>null</code> if no annotated class can be found
     * @deprecated Use the {@link #getClassFor(String) getClassFor} method.
     */
    Class<? extends HippoBean> getAnnotatedClassFor(String jcrPrimaryNodeType);

    /**
     * @param jcrPrimaryNodeType Primary node type
     * @return <code>Class</code> for this jcrPrimaryNodeType or <code>null</code> if no annotated class can be found
     */
    default Class<? extends HippoBean> getClassFor(String jcrPrimaryNodeType) {
        return getAnnotatedClassFor(jcrPrimaryNodeType);
    }

    String getPrimaryNodeTypeNameFor(Class<? extends HippoBean> hippoBean);
}
