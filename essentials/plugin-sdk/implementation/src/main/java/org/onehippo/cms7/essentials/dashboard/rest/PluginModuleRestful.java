/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.rest;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;
import org.onehippo.cms7.essentials.dashboard.model.Restful;

import io.swagger.annotations.ApiModel;

/**
 * Plugin javascript module descriptor.
 * Contains application name and plugin's javascript references.
 */

@ApiModel
@XmlRootElement(name = "module")
public class PluginModuleRestful implements Restful {

    public static final String DEFAULT_APP_NAME = "hippo.essentials";

    private static final long serialVersionUID = 1L;

    private String application = DEFAULT_APP_NAME;
    private List<String> files;

    public void addFiles(final PluginDescriptor descriptor) {
        if (PluginDescriptor.TYPE_TOOL.equals(descriptor.getType())
            || descriptor.getHasConfiguration()) {
            if (files == null) {
                files = new ArrayList<>();
            }
            final String pluginId = descriptor.getId();
            files.add(descriptor.getType() + "/" + pluginId + "/" + pluginId + ".js");
        }
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(final List<String> files) {
        this.files = files;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(final String application) {
        this.application = application;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginModuleRestful{");
        sb.append("files=").append(files);
        sb.append(", application='").append(application).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
