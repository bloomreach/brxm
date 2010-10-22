package org.hippoecm.hst.jaxrs.model.content;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;

public abstract class AbstractNodeRepresentationDataset extends AbstractDataset {
    
    private static final long serialVersionUID = 1L;
    
    private List<? extends NodeRepresentation> nodeRepresentations;
    private List<Link> links;
    
    public AbstractNodeRepresentationDataset() {
        
    }
    
    protected List<? extends NodeRepresentation> getNodeRepresentations() {
        return nodeRepresentations;
    }
    
    public void setNodeRepresentations(List<? extends NodeRepresentation> nodeRepresentations) {
        this.nodeRepresentations = nodeRepresentations;
    }
    
    @XmlElementWrapper(name="links")
    @XmlElements(@XmlElement(name="link"))
    public List<Link> getLinks() {
        return links;
    }
    
    public void setLinks(List<Link> links) {
        this.links = links;
    }
    
    public void addLink(Link link) {
        if (links == null) {
            links = new ArrayList<Link>();
        }
        
        links.add(link);
    }
}
