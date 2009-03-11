package org.hippoecm.hst.ocm;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.hippoecm.hst.ocm.NodeAware;
import org.hippoecm.hst.ocm.SimpleObjectConverter;
import org.hippoecm.hst.ocm.SimpleObjectConverterAware;

public class HippoStdNode implements NodeAware, SimpleObjectConverterAware {

    protected transient javax.jcr.Node node;
    protected transient SimpleObjectConverter simpleObjectConverter;
    protected String path;

    public javax.jcr.Node getNode() {
        return this.node;
    }
    
    public void setNode(javax.jcr.Node node) {
        this.node = node;
    }
    
    public SimpleObjectConverter getSimpleObjectConverter() {
        return this.simpleObjectConverter;
    }
    
    public void setSimpleObjectConverter(SimpleObjectConverter simpleObjectConverter) {
        this.simpleObjectConverter = simpleObjectConverter;
    }
    
    @Field(path=true)
    public String getPath() {
        return this.path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        String name = "";
        
        if (this.node != null) {
            try {
                name = this.node.getName();
            } catch (Exception e) {
            }
        }

        return name;
    }

}
