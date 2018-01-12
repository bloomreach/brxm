/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onehippo.cms7.essentials.plugins.urlrewriter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.install.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.service.model.Module;
import org.onehippo.cms7.essentials.plugin.sdk.service.WebXmlService;

/**
 * Modify the site's web.xml to install the rewrite filter.
 */
public class UrlRewriterInstruction implements Instruction {

    private static final String FILTER_CLASS = "org.onehippo.forge.rewriting.HippoRewriteFilter";
    private static final String FILTER_NAME = "RewriteFilter";
    private static final List<String> URL_PATTERNS = Collections.singletonList("/*");
    private static final List<WebXmlService.Dispatcher> DISPATCHERS = Arrays.asList(WebXmlService.Dispatcher.REQUEST, WebXmlService.Dispatcher.FORWARD);
    private static final Module MODULE = Module.SITE;
    private static final Map<String, String> initParams = new HashMap<>();

    static {
        // See HippoRewriteFilter for documentation of initParams
        initParams.put("rulesLocation", "/content/urlrewriter");
        initParams.put("logLevel", "slf4j");
        initParams.put("statusEnabled", "true");
        initParams.put("statusPath", "/rewrite-status");
        initParams.put("statusEnabledOnHosts", "localhost, 127.0.0.*, *.lan, *.local");
    }

    @Inject private WebXmlService webXmlService;

    @Override
    public Status execute(final PluginContext context) {
        if (webXmlService.addFilter(MODULE, FILTER_NAME, FILTER_CLASS, initParams)
                && webXmlService.addFilterMapping(MODULE, FILTER_NAME, URL_PATTERNS)
                && webXmlService.addDispatchersToFilterMapping(MODULE, FILTER_NAME, DISPATCHERS)
                && webXmlService.addDispatchersToFilterMapping(MODULE, "HstFilter", DISPATCHERS)) {
            return Status.SUCCESS;
        }
        return Status.FAILED;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Install URL Rewriter filter into Site web.xml.");
    }
}
