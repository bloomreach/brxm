package org.hippoecm.hst.plugins.frontend.editor.domain;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.Resource;
import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.util.resource.IResourceStream;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Description implements Descriptive {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(Description.class);

    private String description;
    private Resource resource;

    JcrNodeModel model;

    public Description(final JcrNodeModel model) {
        this.model = model;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Resource getIconResource() {
        final Node node = model.getNode();
        try {
            if (resource == null && node.hasNode("hst:icon")) {
                //final IResourceStream resourceStream = new JcrResourceStream(model.getNode().getNode("hst:icon"));
                resource = new WebResource() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public IResourceStream getResourceStream() {
                        try {
                            return new JcrResourceStream(node.getNode("hst:icon"));
                        } catch (PathNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (RepositoryException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        return null;
                    }

                };
            }
        } catch (RepositoryException e) {
            log.error("Error retrieving node hst:icon from node {0}", model.getItemModel().getPath());
        }

        return resource;
    }

    public void setIconResource(Resource resource) {
        this.resource = resource;
    }

}
