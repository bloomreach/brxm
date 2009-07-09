package org.hippoecm.hst.plugins.frontend.editor.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Resource;
import org.hippoecm.frontend.model.JcrNodeModel;

public class Template extends EditorBean implements Descriptive {
    private static final long serialVersionUID = 1L;

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
