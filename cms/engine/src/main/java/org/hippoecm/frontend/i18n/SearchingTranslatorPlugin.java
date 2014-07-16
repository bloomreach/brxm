/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.i18n;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchingTranslatorPlugin extends AbstractTranslateService implements IPlugin {

    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(SearchingTranslatorPlugin.class);

    public SearchingTranslatorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    public IModel getModel(Map<String, String> criteria) {
        String  strQuery = "//element(*, " + HippoNodeType.NT_TRANSLATED+ ")[fn:name()='"
                + StringCodecFactory.ISO9075Helper.encodeLocalName(NodeNameCodec.encode(criteria.get(HippoNodeType.HIPPO_KEY)))
                + "']/element(" + HippoNodeType.NT_TRANSLATION + ", " + HippoNodeType.HIPPO_TRANSLATION + ")[@" +
                HippoNodeType.HIPPO_LANGUAGE + "='" + NodeNameCodec.encode(criteria.get(HippoNodeType.HIPPO_LANGUAGE)) + "']";
        try {
            QueryManager qMgr = UserSession.get().getQueryManager();
            Query query = qMgr.createQuery(strQuery, Query.XPATH);
            NodeIterator nodes = query.execute().getNodes();
            Set<NodeWrapper> list = new HashSet<NodeWrapper>();
            while (nodes.hasNext()) {
                list.add(new NodeWrapper(nodes.nextNode(), criteria));
            }
            if (list.size() > 0) {
                return new TranslationSelectionStrategy<IModel>(criteria.keySet()).select(list).getModel();
            }
        } catch (InvalidQueryException ex) {
            log.info("For criteria '{}' the xpath query '{}' is not valid : {}", criteria.toString(), strQuery, ex.toString());
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.warn("RepositoryException", ex);
            } else {
                log.warn("RepositoryException : {}", ex.toString());
            }
        }
        return null;
    }

    public void start() {
    }

    public void stop() {
    }

}
