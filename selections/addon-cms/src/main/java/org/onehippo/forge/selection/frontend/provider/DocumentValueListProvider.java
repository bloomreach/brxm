/*
 * Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.onehippo.forge.selection.frontend.provider;

import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.utils.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentValueListProvider extends Plugin implements IValueListProvider {

    /** Deprecated because the method that uses it is deprecated */
    @Deprecated
    private final static String CONFIG_SOURCE = "source";

    private static final Logger log = LoggerFactory.getLogger(DocumentValueListProvider.class);

    public DocumentValueListProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String name = config.getString(IValueListProvider.SERVICE, IValueListProvider.SERVICE_VALUELIST_DEFAULT);
        context.registerService(this, name);

        if (log.isDebugEnabled()) {
            log.debug(this.getClass().getName() + " registered under " + name);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ValueList getValueList(IPluginConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Argument 'config' may not be null");
        }
        return getValueList(config.getString(CONFIG_SOURCE));
    }

    /** {@inheritDoc} */
    @Override
    public ValueList getValueList(String name) {
        return getValueList(name, null/*locale*/);
    }

    /** {@inheritDoc} */
    @Override
    public ValueList getValueList(final String name, final Locale locale) {
        return getValueList(name, locale, obtainSession());
    }

    @Override
    public ValueList getValueList(final String name, final Locale locale, final Session session) {
        return JcrUtils.getValueList(name, locale, session);
    }

    @Override
    public List<String> getValueListNames() {
        return JcrUtils.getValueListNames(obtainSession());
    }

    /**
     * Gets the configured JCR node that holds the values for the select.
     *
     * @param nodeSource path to the source node
     * @return {@link Node}
     */
    protected Node getSourceNode(final String nodeSource) {
        return JcrUtils.getSourceNode(nodeSource, obtainSession());
    }

    /**
     * Get a possible variant of a source node specified by locale.
     * */
    protected Node getSourceNodeVariant(final Node sourceNode, final Locale locale) {
        return JcrUtils.getSourceNodeVariant(sourceNode, locale);
    }

    /**
     * Gets the JCR {@link Session} from the Wicket
     * {@link org.apache.wicket.Session}
     *
     * @return {@link Session}
     */
    protected Session obtainSession() {
        UserSession userSession = (UserSession) org.apache.wicket.Session.get();
        return userSession.getJcrSession();
    }

}