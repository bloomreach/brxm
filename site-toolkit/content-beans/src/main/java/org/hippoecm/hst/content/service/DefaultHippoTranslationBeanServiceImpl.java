/**
 * Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Hippo Translation Content Bean service implementation by executing queries simply to find all the
 * Hippo Translation beans.
 */
public class DefaultHippoTranslationBeanServiceImpl implements HippoTranslationBeanService {

    private static Logger log = LoggerFactory.getLogger(DefaultHippoTranslationBeanServiceImpl.class);

    public DefaultHippoTranslationBeanServiceImpl() {
    }

    @Override
    public <T extends HippoBean> Map<String, T> getTranslationBeans(Session session, String translationId,
            Class<T> beanMappingClass) throws RepositoryException {
        Map<String, T> translations = new LinkedHashMap<String, T>();

        final List<Node> translationNodes = getTranslationNodes(session, translationId);
        final HstRequestContext requestContext = RequestContextProvider.get();
        final ObjectConverter objectConverter = requestContext.getContentBeansTool().getObjectConverter();

        for (Node translationNode : translationNodes) {
            String locale = translationNode.getProperty(HippoTranslationNodeType.LOCALE).getString();

            try {
                Object bean = objectConverter.getObject(translationNode);

                if (bean != null) {
                    if (beanMappingClass != null) {
                        if (beanMappingClass.isAssignableFrom(bean.getClass())) {
                            translations.put(locale, (T) bean);
                        } else {
                            log.debug("Skipping bean of type '{}' because not of beanMappingClass '{}'",
                                    bean.getClass().getName(), beanMappingClass.getName());
                        }
                    } else {
                        translations.put(locale, (T) bean);
                    }
                }
            } catch (ObjectBeanManagerException e) {
                log.warn("Skipping bean: {}", e);
            }
        }

        return translations;
    }

    @Override
    public List<Node> getTranslationNodes(final Session session, final String translationId)
            throws RepositoryException {
        if (StringUtils.isBlank(translationId)) {
            throw new IllegalArgumentException("Blank translation ID.");
        }

        List<Node> translationNodes = new ArrayList<>();

        String xpath = "//element(*," + HippoTranslationNodeType.NT_TRANSLATED + ")[" + HippoTranslationNodeType.ID
                + " = '" + translationId + "']";

        @SuppressWarnings("deprecation")
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        final QueryResult result = query.execute();

        Node translationNode;
        for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext();) {
            translationNode = nodeIt.nextNode();

            if (translationNode != null) {
                if (!translationNode.hasProperty(HippoTranslationNodeType.LOCALE)) {
                    log.debug("Skipping node '{}' because does not contain property '{}'", translationNode.getPath(),
                            HippoTranslationNodeType.LOCALE);
                    continue;
                }

                translationNodes.add(translationNode);
            }
        }

        return translationNodes;
    }

}
