/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class BluePrintService implements BluePrint {

    private final String id;
    private final String cmsPluginClass;
    private final String parameterInfoClass;
    private final String name;
    private final String description;

    public BluePrintService(final Node bluePrint) throws RepositoryException {

        id = bluePrint.getName();

        if (bluePrint.hasProperty("hst:name")) {
            this.name = bluePrint.getProperty("hst:name").getString();
        } else {
            this.name = this.id;
        }

        if (bluePrint.hasProperty("hst:description")) {
            this.description = bluePrint.getProperty("hst:description").getString();
        } else {
            this.description = null;
        }

        if (bluePrint.hasProperty("hst:pluginclass")) {
            cmsPluginClass = bluePrint.getProperty("hst:pluginClass").getString();
        } else {
            cmsPluginClass = null;
        }

        if (bluePrint.hasProperty("hst:parameterinfoclass")) {
            parameterInfoClass = bluePrint.getProperty("hst:parameterinfoclass").getString();
        } else {
            parameterInfoClass = null;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getCmsPluginClass() {
        return cmsPluginClass;
    }

    @Override
    public String getParameterInfo() {
        return parameterInfoClass;
    }
}
