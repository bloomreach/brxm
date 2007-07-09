package org.hippocms.repository.webapp.menu;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;

public abstract class AbstractDialogPage extends WebPage {

    public AbstractDialogPage(final AbstractDialog dialog) {
        add(new AjaxLink("ok") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                ok();
                dialog.close(target);
            }
        });
        
        add(new AjaxLink("cancel") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                cancel();
                dialog.close(target);
            }
        });
    }
    
    
    protected abstract void ok();

    protected abstract void cancel();
    
}
