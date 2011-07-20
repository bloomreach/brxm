package org.hippoecm.frontend.plugins.console.menu.save;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.session.UserSession;

public class SaveDialogLink extends DialogLink {

    private static final long serialVersionUID = 1L;

    public SaveDialogLink(String id, IModel linktext, final IDialogFactory dialogFactory, final IDialogService dialogService) {
        super(id, linktext, dialogFactory, dialogService);
        Label label = new Label("dialog-link-text-extended", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;
            @Override
            public String getObject() {
                if (hasSessionChanges()) {
                    return "*";
                }
                return "";
            }
        });
        label.setOutputMarkupId(true);
        link.add(label);
    }
    
    private boolean hasSessionChanges() {
        Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
        try {
            return session.hasPendingChanges();
        } catch (RepositoryException e) {
            return false;
        }
    }

}
