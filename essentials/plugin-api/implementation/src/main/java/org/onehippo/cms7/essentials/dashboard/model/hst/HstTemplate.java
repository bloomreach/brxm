package org.onehippo.cms7.essentials.dashboard.model.hst;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentMultiProperty;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentProperty;

/**
 */
@PersistentNode(type = "hst:template")
public class HstTemplate extends BaseJcrModel {
    private static final String HST_TEMPLATES = "hst:templates";
    private static final String HST_TEMPLATE = "hst:template";
    private static final String HST_CONTAINERS = "hst:containers";
    private static final String HST_RENDER_PATH = "hst:renderpath";
    private static final String HST_SCRIPT = "hst:script";
    private static final String HST_IS_NAMED = "hst:isnamed";


    @PersistentMultiProperty(name = HST_CONTAINERS)
    private List<String> containers = new ArrayList<>();
    @PersistentProperty(name = HST_RENDER_PATH)
    private String renderPath = null;
    @PersistentProperty(name = HST_SCRIPT)
    private String script = null;
    private boolean isNamed = false;

    protected HstTemplate(final String name) {
        setName(name);
    }

    protected HstTemplate(final String name, final String renderPath) {
        this(name);
        this.renderPath = renderPath;
    }

    public List<String> getContainers() {
        return containers;
    }

    public void setContainers(final List<String> containers) {
        this.containers = containers;
    }

    public String getRenderPath() {
        return renderPath;
    }

    public void setRenderPath(final String renderPath) {
        this.renderPath = renderPath;
    }

    public String getScript() {
        return script;
    }

    public void setScript(final String script) {
        this.script = script;
    }

    public boolean isNamed() {
        return isNamed;
    }

    public void setNamed(final boolean named) {
        isNamed = named;
    }

    public static Node createTemplateNode(final Node parent, final HstTemplate template) throws RepositoryException, TemplateExistsException {
        if (parent == null) {
            throw new RepositoryException("Parent is not available.");
        }
        if (!parent.isNodeType(HST_TEMPLATES)) {
            throw new RepositoryException("Parent is no templates node.");
        }

        if (parent.hasNode(template.getName())) {
            throw new TemplateExistsException("Template already exists.");
        }

        final Node templateNode = parent.addNode(template.getName(), HST_TEMPLATE);
        if (template.getRenderPath() != null) {
            templateNode.setProperty(HST_RENDER_PATH, template.getRenderPath());
        }
        if (template.getScript() != null) {
            templateNode.setProperty(HST_SCRIPT, template.getScript());
        }
        if (template.isNamed()) {
            templateNode.setProperty(HST_IS_NAMED, template.isNamed());
        }
        if (template.getContainers().size() > 0) {
            templateNode.setProperty(HST_CONTAINERS, template.getContainers().toArray(new String[template.getContainers().size()]));
        }
        return templateNode;
    }

}
