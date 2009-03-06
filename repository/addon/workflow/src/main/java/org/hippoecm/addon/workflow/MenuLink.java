package org.hippoecm.addon.workflow;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.StringResourceModel;

public abstract class MenuLink extends AjaxLink {
    public MenuLink(String id) {
        super(id);
    }

    public MenuLink(String id, StringResourceModel model) {
        super(id, model);
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        ((MenuButton)findParent(MenuButton.class)).collapse(target);
        onClick();
    }

    public abstract void onClick();
}
