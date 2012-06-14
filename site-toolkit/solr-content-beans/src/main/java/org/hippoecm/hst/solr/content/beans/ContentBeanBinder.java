/*
 *  Copyright 2012 Hippo.
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

package org.hippoecm.hst.solr.content.beans;


import java.util.List;

import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;

public interface ContentBeanBinder {

    /**
     *
     * @return the List of {@link IdentifiableContentBean}s that this @{@link ContentBeanBinder} can provide (the content for)
     *
     */
     List<Class<? extends IdentifiableContentBean>> getBindableClasses();

    /**
     * This method will be invoked during {@link org.hippoecm.hst.solr.content.beans.query.Hit#getBean()} when the 
     * {@link org.hippoecm.hst.solr.content.beans.query.HippoQueryResult} had its {@link org.hippoecm.hst.solr.content.beans.query.HippoQueryResult#bindHits()} or
     * {@link org.hippoecm.hst.solr.content.beans.query.HippoQueryResult#bindHits(java.util.List)} invoked *AND* when the <code>identifiableContentBean</code>
     * class is contained in {@link #getBindableClasses()}
     *  
     * Implementation of this {@link ContentBeanBinder} can in the {@link #callbackHandler(org.hippoecm.hst.content.beans.standard.IdentifiableContentBean)}
     * implement the logic to bind the {@link IdentifiableContentBean} back to its original provider
     * 
     * @param identifiableContentBean the {@link IdentifiableContentBean} instance
     * @throws BindingException when the <code>identifiableContentBean</code> cannot be bound 
     */
    void callbackHandler(IdentifiableContentBean identifiableContentBean) throws BindingException;
}
