package org.hippoecm.frontend.plugins.xinha.modal.browse;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.browse.AbstractBrowseView;
import org.hippoecm.frontend.plugins.xinha.modal.Dialog;
import org.hippoecm.frontend.plugins.xinha.modal.DialogBehavior;
import org.hippoecm.frontend.plugins.xinha.modal.IDialog;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class BrowserPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;
    
    private AbstractBrowseView browserView;
    
    final protected Form form;
    final protected AjaxButton ok;
    final protected AjaxButton cancel;
    final protected FeedbackPanel feedback;

    public BrowserPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);
        
        browserView = new AbstractBrowseView(context, config) {
            private static final long serialVersionUID = 1L;
            
            @Override
            protected String getExtensionPoint() {
                return config.getString("dialog.list");
            }
        };
        
        add(form = new Form("form"));

        form.add(ok = new AjaxButton("ok", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                getDialog().ok(target);
            }

        });
        
        form.add(cancel = new AjaxButton("close", form) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                getDialog().cancel(target);
            }
        });
        
        //TODO: feedback is written in the page feedbackpanel, not this one in the modalwindow
        form.add(feedback = new FeedbackPanel("dialog.feedback"));
        feedback.setOutputMarkupId(true);
    }
    
    private IDialog getDialog() {
        String id = getPluginConfig().getString(DialogBehavior.DIALOG_SERVICE_ID);
        return getPluginContext().getService(id, IDialog.class);
    }
    
    
    
}
