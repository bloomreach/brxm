package org.hippoecm.repository.frontend.wysiwyg.xinha;

import java.io.Serializable;
import java.util.Map;

public class XinhaEditorConf implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String[] plugins;
    private Map configuration;
    
    public XinhaEditorConf()
    {
        
    }
    
    public XinhaEditorConf(String name, String[] plugins, Map configuration)
    {
        this.name = name;
        this.plugins = plugins;
        this.configuration = configuration;
    }
    
    public void setPlugins(String[] value)
    {
        this.plugins = value;
    }
    
    public String[] getPlugins()
    {
        return plugins;
    }
    
    public void setConfiguration(Map value)
    {
        this.configuration = value;
    }
    
    public Map getConfiguration()
    {
        return configuration;
    }
    
    public void setName(String value)
    {
        this.name = value;
    }
    
    public String getName()
    {
        return name;
    }
}