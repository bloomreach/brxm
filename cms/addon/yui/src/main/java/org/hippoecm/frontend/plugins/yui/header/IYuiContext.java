package org.hippoecm.frontend.plugins.yui.header;

import java.util.Map;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.header.templates.FinalTextTemplate;
import org.onehippo.yui.YuiNamespace;

public interface IYuiContext extends IHeaderContributor {
    
    /**
     * Add a YUI module from the YAHOO namespace 
     * @param module YUI module name
     */
    void addModule(String module);
    
    /**
     * Add a YUI module from the specified namespace
     * @param ns Namespace to load module from
     * @param module Module name
     */
    void addModule(YuiNamespace ns, String module);
    
    /**
     * Add a static template to the response. The model provided by the parameters map is final. 
     * @param clazz
     * @param filename
     * @param parameters
     */
    void addTemplate(Class<?> clazz, String filename, Map<String, Object> parameters );
    
    /**
     * 
     * @param template
     */
    void addTemplate(FinalTextTemplate template);
    
    /**
     * Add a dynamic template to the response. The model will be refreshed upon every request.   
     * @param template
     */
    void addTemplate(DynamicTextTemplate template);
    
    /**
     * Add javascript that get's executed on pageLoad
     * @param string String of javascript code that is evaluated on the client  
     */
    void addOnload(String string);
    
    void addJavascriptReference(ResourceReference reference);

    void addCssReference(ResourceReference reference);
    
}
