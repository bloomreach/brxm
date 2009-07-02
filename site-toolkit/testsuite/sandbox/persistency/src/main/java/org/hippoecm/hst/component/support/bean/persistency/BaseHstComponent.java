/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.component.support.bean.persistency;

import java.util.Map;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.PersistableObjectBeanManagerImpl;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.persistence.ContentNodeBinder;
import org.hippoecm.hst.persistence.ContentPersistenceManager;

/**
 * A base HstComponent implementation to provide some facility methods for accessing content node POJO objects,
 * {@link ObjectBeanManager}, {@link ContentPersistenceManager}, request parameters, query manager, etc.
 * <P>
 * This implementation enables developers to make use of HST Content Bean's {@link ObjectBeanManager}
 * which provides a simple object-content mapping solution.
 * To use {@link ObjectBeanManager}, you can invoke {@link #getObjectBeanManager(HstRequest)}, which retrieves
 * a JCR session from {@link HstRequestContext#getSession()} internally.
 * </P>
 * <P>
 * In most read-only cases, the {@link ObjectBeanManager} could be enough to access content objects.
 * However, if you want to persist content POJO objects back to the repository, you need one more: {@link ContentPersistenceManager}.
 * You can create an instance of <CODE>ContentPersistenceManager</CODE> by using one of <CODE>getContentPersistenceManager()</CODE> methods.
 * These methods require {@link javax.jcr.Session} because the persistency may require another session with different credentials.
 * You can get a session by {@link HstRequestContext#getSession()} or you can impersonate the session to another session
 * by {@link javax.jcr.Session#impersonate(javax.jcr.Credentials)} to persist the content object.
 * </P>
 * <P>
 * <EM>Note: if you retrieve another JCR session to persist contents by invoking {@link javax.jcr.Session#impersonate(javax.jcr.Credentials)}, 
 * then you are responsible to invoke <CODE>logout()</CODE> after persisting. The HST container will not care the 
 * impersonated session from the application side.</EM>
 * </P>
 * 
 * @version $Id$
 */
public class BaseHstComponent extends org.hippoecm.hst.component.support.bean.BaseHstComponent {
    
    public ContentPersistenceManager getContentPersistenceManager(Session session) {
        return getContentPersistenceManager(session, null);
    }
    
    public ContentPersistenceManager getContentPersistenceManager(Session session, Map<String, ContentNodeBinder> contentNodeBinders) {
        return getContentPersistenceManager(session, contentNodeBinders, false);
    }
    
    public ContentPersistenceManager getContentPersistenceManager(Session session, boolean publishAfterUpdate) {
        return getContentPersistenceManager(session, null, publishAfterUpdate);
    }
    
    public ContentPersistenceManager getContentPersistenceManager(Session session, Map<String, ContentNodeBinder> contentNodeBinders, boolean publishAfterUpdate) {
        PersistableObjectBeanManagerImpl cpm = new PersistableObjectBeanManagerImpl(session, this.objectConverter, contentNodeBinders);
        cpm.setPublishAfterUpdate(publishAfterUpdate);
        return cpm;
    }
    
}
