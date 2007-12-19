package org.hippoecm.repository.frontend.wysiwyg.xinha;

import java.util.Iterator;
import java.util.Vector;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;

public class XinhaEditorConfigurationBehaviour extends AbstractHeaderContributor
{
    private static final long serialVersionUID = 1L;
    
    private static XinhaEditorConfigurationBehaviour instance = null;
    
    private IHeaderContributor xinhaCoreContributor = HeaderContributor.forJavaScript(new ResourceReference(XinhaEditor.class, "impl/XinhaCore.js"));
    
    private Vector configurations;
    private XinhaEditorConfigurationHeaderContributor configurationContributor = new XinhaEditorConfigurationHeaderContributor();
    private XinhaEditorInitialConfigurationHeaderContributor initialConfigurationContributor = new XinhaEditorInitialConfigurationHeaderContributor();
    private String implPath;
    
	private XinhaEditorConfigurationBehaviour(String implPath)
	{
	    this.implPath = implPath;
	    configurations = new Vector();
	}
	
	private class XinhaEditorInitialConfigurationHeaderContributor implements IHeaderContributor
	{
	    private static final long serialVersionUID = 1L;
        
        public XinhaEditorInitialConfigurationHeaderContributor() {}
        
        public void renderHead(IHeaderResponse response) {
            StringBuffer buff = new StringBuffer();
            buff.append("_editor_url  = '" + "http://localhost:8080/browser/" + implPath + "';");
            buff.append("_editor_lang = 'en';");
            response.renderJavascript(buff, null);
        }
	}
	    
	private class XinhaEditorConfigurationHeaderContributor implements IHeaderContributor
    {
	    private static final long serialVersionUID = 1L;
	    
	    public XinhaEditorConfigurationHeaderContributor() {}
	    
        public void renderHead(IHeaderResponse response) {
            
            StringBuffer buff = new StringBuffer();
            
            buff.append("xinha_plugins = null;xinha_config = null; function xinha_init(){");
            buff.append("xinha_plugins = [");
            
            Iterator it = configurations.iterator();
            while(it.hasNext()) {
                XinhaEditorConf conf = (XinhaEditorConf)it.next();
                String[] plugin = conf.getPlugins();
                for(int i=0; i<plugin.length-1; i++) {
                    buff.append("'"+plugin[i] + "',");
                }
                buff.append("'"+plugin[plugin.length-1]+"'");
            }
            buff.append("];");
            
            it = configurations.iterator();
            buff.append("xinha_editors = [");
            while(it.hasNext()) {
                XinhaEditorConf conf = (XinhaEditorConf)it.next();
                if(it.hasNext()) {
                    buff.append("'" + conf.getName() + "'" + ',');
                }
                else {
                    buff.append("'" + conf.getName() + "'");
                }
            }
            buff.append("];");
            
            buff.append("xinha_config = new Xinha.Config();");
            buff.append("xinha_editors = Xinha.makeEditors(xinha_editors, xinha_config, xinha_plugins);");
            
            /*it = configurations.iterator();
            while(it.hasNext()) {
                XinhaEditorConf conf = (XinhaEditorConf)it.next();
                String[] plugin = conf.getPlugins();
                String plugins = "";
                for(int i=0; i<plugin.length-1; i++) {
                    plugins += "'"+plugin[i] + "',";
                }
                plugins += "'"+plugin[plugin.length-1]+"'";
                buff.append("xinha_editors['"+conf.getName()+"'].registerPlugins(["+plugins+"]);");
            }*/
            
            it = configurations.iterator();
            while(it.hasNext()) {
                XinhaEditorConf conf = (XinhaEditorConf)it.next();
                Iterator key = conf.getConfiguration().keySet().iterator();
                Iterator value = conf.getConfiguration().values().iterator();
                while(key.hasNext()) {
                    buff.append("xinha_editors."+conf.getName()+".config."+key.next()+"='"+value.next()+"';");
                }
            }
            
            buff.append("Xinha.startEditors(xinha_editors);");
            
            buff.append("}");
           
            response.renderJavascript(buff, null);
            response.renderOnLoadJavascript("xinha_init()");
        }
    }
	
	public static XinhaEditorConfigurationBehaviour getInstance(String implPath)
    {
	    if(instance == null) {
	        return new XinhaEditorConfigurationBehaviour(implPath);
	    }
	    else {
	        return instance;
	    }
    }
	
	public void addConfiguration(XinhaEditorConf o)
    {
        configurations.add(o);
    }
	
    public final IHeaderContributor[] getHeaderContributors()
    {
        return new IHeaderContributor[] { initialConfigurationContributor, xinhaCoreContributor, configurationContributor };
    }
	
}