/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.ctx;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;

import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.JcrPluginConfigService;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

/**
 * @version "$Id$"
 */
public class PanelPluginContext extends DashboardPluginContext {

    private static final Logger log = LoggerFactory.getLogger(PanelPluginContext.class);

    private static final long serialVersionUID = 1L;

    public PanelPluginContext(final Session session, final Plugin plugin) {
        super(session, plugin);
    }


    @Override
    public Plugin getDescriptor() {
        throw new UnsupportedOperationException("Panel plugin doesn't support plugin descriptor");
    }

}
