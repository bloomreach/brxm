package org.hippoecm.hst.plugins.frontend.editor.sitemap;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.RadioGroupWidget;
import org.hippoecm.hst.plugins.frontend.editor.dao.EditorDAO;
import org.hippoecm.hst.plugins.frontend.editor.dialogs.AddNodeDialog;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemapItem;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemapItem.Matcher;
import org.hippoecm.hst.plugins.frontend.editor.validators.UniqueSitemapItemValidator;

public class AddSitemapItemDialog extends AddNodeDialog<SitemapItem> {
    private static final long serialVersionUID = 1L;

    final List<Matcher> matchers = Arrays.asList(new Matcher[] { Matcher.CUSTOM, Matcher.WILDCARD, Matcher.INFINITE });

    private Matcher selection = Matcher.CUSTOM;

    public AddSitemapItemDialog(EditorDAO<SitemapItem> dao, RenderPlugin plugin, JcrNodeModel parent) {
        super(dao, plugin, parent);

        //Init textfield value with new node name
        Matcher.CUSTOM.setValue(getBean().getMatcher());

        final FormComponent textField = new RequiredTextField("matcher") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return selection == Matcher.CUSTOM;
            }
        };
        textField.setOutputMarkupId(true);
        textField.add(new UniqueSitemapItemValidator(this, dao.getHstContext().sitemap));
        add(textField);
        setFocus(textField);

        add(new RadioGroupWidget("choices", matchers, new PropertyModel(this, "selection")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onChange(AjaxRequestTarget target, Object object) {
                getBean().setMatcher(selection.getValue());
                target.addComponent(textField);
            }
        });

    }
}
