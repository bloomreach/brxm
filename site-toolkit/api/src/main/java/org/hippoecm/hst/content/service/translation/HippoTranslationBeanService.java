/**
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.content.service.translation;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * Hippo Translation Content Bean service.
 */
public interface HippoTranslationBeanService {

    /**
     * Returns all the Hippo Translation Beans of {@code translationId}.
     * @param session JCR session
     * @param translationId the translation ID
     * @param beanMappingClass expected mapping class of Hippo Translation Beans
     * @return all the Hippo Translation Beans keyed by locale string in the same {@code translationId}
     * @throws RepositoryException if any repository exception occurs
     */
    <T extends HippoBean> Map<String, T> getTranslationBeans(Session session, String translationId,
            Class<T> beanMappingClass) throws RepositoryException;

    /**
     * Returns all the Hippo Translation nodes of {@code translationId}.
     * @param session JCR session
     * @param translationId the translation ID
     * @return all the Hippo Translation nodes in the same {@code translationId}
     * @throws RepositoryException if any repository exception occurs
     */
    List<Node> getTranslationNodes(Session session, String translationId) throws RepositoryException;

}
