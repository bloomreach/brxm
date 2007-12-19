package org.hippoecm.repository.frontend.wysiwyg.xinha;

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.repository.frontend.wysiwyg.HtmlEditor;

public class XinhaEditor extends HtmlEditor
{
    private static final long serialVersionUID = 1L;
    
    /*private static final ResourceReference JS = new ResourceReference(
            XinhaEditor.class, "impl/xinha/XinhaCore.js");*/
    
    private TextArea editor;
    private XinhaEditorConf editorConf; 
    
    private final AbstractDefaultAjaxBehavior postBehaviour;
    private XinhaEditorConfigurationBehaviour bh;
    
    public XinhaEditor(final String id, XinhaEditorConfigurationBehaviour bh)
    {
        super(id);
        
        this.bh = bh;
        
        editor = new TextArea("editor", getModel());
        editor.setOutputMarkupId(true);
        editor.setVisible(true);
        
        postBehaviour = new AbstractDefaultAjaxBehavior() {
            
            private static final long serialVersionUID = 1L;

            protected void respond(AjaxRequestTarget target)
            {
                RequestCycle requestCycle = RequestCycle.get();
                boolean save = Boolean.valueOf(requestCycle.getRequest().getParameter("save"))
                        .booleanValue();

                if (save)
                {
                    editor.processInput();
                       
                    System.out.println("editor value: "+editor.getValue());
                    System.out.println("editor contents: "+XinhaEditor.this.getContent());
                    
                    if (editor.isValid())
                    {
                        System.out.println("ALLES GOED");
                    }
                }
            }
        };
        
        editor.add(postBehaviour);
        
        editorConf = new XinhaEditorConf();
        //conf.setName(editor.getMarkupId());
        editorConf.setName("editor2");
        editorConf.setPlugins(new String[]{"WicketSave",
                                         "CharacterMap",
                                         "ContextMenu",
                                         "ListType",
                                         "SpellChecker",
                                         "Stylist",
                                         "SuperClean",
                                         "TableOperations"});

//        IModel variablesModel = new AbstractReadOnlyModel() {
//            private static final long serialVersionUID = 1L;
//            /** cached variables; we only need to fill this once. */
//            private Map variables;
//
//            public Object getObject() {
//              if (variables == null) {
//                this.variables = new MiniMap(2);
//                variables.put("editors", "'"+editor.getMarkupId()+"'");
//                variables.put("postUrl", postBehaviour.getCallbackUrl());
//              }
//              return variables;
//            }
//        };
//        
//        HeaderContributor jsConfig = new HeaderContributor(new IHeaderContributor() {
//          private static final long serialVersionUID = 1L;
//          public void renderHead(IHeaderResponse response) {
//              StringBuffer buff = new StringBuffer();
//              buff.append("_editor_url  = '" +
//                          XinhaEditor.this.urlFor(new ResourceReference(XinhaEditor.class, "impl/xinha/")) + 
//                          "';");
//              buff.append("_editor_lang = 'en';");
//              response.renderJavascript(buff, null);
//              response.renderOnLoadJavascript("xinha_init()");
//          }
//        });
//        
//        if(editorCount == 1) {
//            add(jsConfig);
//            add(HeaderContributor.forJavaScript(JS));
//        }
//        
//        add(TextTemplateHeaderContributor.forJavaScript(
//                XinhaEditor.class, "config.js", variablesModel));
//        
        
        add(editor);
    }
    
    public XinhaEditorConf getConfiguration()
    {
        return editorConf;
    }
    
    public void init()
    {
        Map conf = new Hashtable();
        conf.put("postUrl", postBehaviour.getCallbackUrl());
        editorConf.setConfiguration(conf);
        
        bh.addConfiguration(editorConf);
    }
    
    private Vector getIFrames()
    {
        final Vector iframes = new Vector();
        getWebPage().visitChildren(new IVisitor(){
            public Object component(Component component) {
                System.out.println(component.getClass().getName());
                   System.out.println(component);
                if(component instanceof ModalWindow) {
                    System.out.println("We hebben een dialog te pakken");
                    iframes.add(component.getMarkupId());
                }
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
        
        return iframes;
    }
}