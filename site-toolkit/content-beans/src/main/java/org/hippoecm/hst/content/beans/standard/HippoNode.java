package org.hippoecm.hst.content.beans.standard;

import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterAware;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;

public class HippoNode implements NodeAware, ObjectConverterAware{
    
    private ObjectConverter objectConverter;
    private Node node;
    protected JCRValueProvider valueProvider;
    
    public void setNode(Node node) {
        this.node = node;
        this.valueProvider = new JCRValueProviderImpl(node);
    }
    
    public Node getNode() {
        return node;
    }
    
    public String getPath(){
       return valueProvider.getPath();
    }
    
    public Map<String, Object> getProperties() {
        return valueProvider.getProperties();
    }
    
    public <T> T getProperty(String name) {
        return (T) getProperties().get(name);
    }
    
    public void setObjectConverter(ObjectConverter objectConverter) {
       this.objectConverter = objectConverter;
    }


    public ObjectConverter getObjectConverter() {
        return objectConverter;
    }

    


}
