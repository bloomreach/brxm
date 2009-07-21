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

package org.hippoecm.hst.plugins.frontend.editor.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Resource;
import org.hippoecm.frontend.model.JcrNodeModel;

public class Template extends EditorBean implements Descriptive {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private String name;
    private String renderPath;
    private List<String> containers;

    private Descriptive descriptive;

    public Template(JcrNodeModel model, Descriptive desc) {
        super(model);
        descriptive = desc;
        containers = new ArrayList<String>();
    }

    public Template(JcrNodeModel model) {
        this(model, new Description(model));
    }

    public String getName() {
        return name;
    }

    public String getRenderPath() {
        return renderPath;
    }

    public void setRenderPath(String renderPath) {
        this.renderPath = renderPath;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getContainers() {
        return containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

    public void addContainer() {
        containers.add("");
    }

    public void removeContainer(int index) {
        containers.remove(index);
    }

    public String getDescription() {
        return descriptive.getDescription();
    }

    public Resource getIconResource() {
        return descriptive.getIconResource();
    }

    public void setDescription(String description) {
        descriptive.setDescription(description);
    }

    public void setIconResource(Resource resource) {
        descriptive.setIconResource(resource);
    }

    @Override
    public void detach() {
        super.detach();
        descriptive.detach();
    }

}
