package org.hippoecm.hst.plugins.frontend.editor.domain;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.model.JcrNodeModel;

public class Template extends EditorBean {
    private static final long serialVersionUID = 1L;

    private String name;
    private String renderPath;
    private List<String> containers;

    public Template(JcrNodeModel model) {
        super(model);
        containers = new ArrayList<String>();
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

}
