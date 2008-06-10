package org.hippoecm.frontend.yui.ajax;

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.WicketAjaxIndicatorAppender;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.wicketstuff.yui.YuiHeaderContributor;

public class AjaxIndicatorBehavior extends AbstractHeaderContributor {
    private static final long serialVersionUID = 1L;

    @Override
    public IHeaderContributor[] getHeaderContributors() {
        IHeaderContributor[] dependencies = new IHeaderContributor[3];
        dependencies[0] = YuiHeaderContributor.forModule("yahoo").getHeaderContributors()[0];
        dependencies[1] = YuiHeaderContributor.forModule("dom").getHeaderContributors()[0];
        dependencies[2] = HeaderContributor.forJavaScript(AjaxIndicatorBehavior.class, "ajax_indicator.js");
        return dependencies;
    }

    @Override
    public void bind(Component component) {

        final WicketAjaxIndicatorAppender ajaxIndicator = new WicketAjaxIndicatorAppender();
        component.add(ajaxIndicator);
        component.add(TextTemplateHeaderContributor.forJavaScript(AjaxIndicatorBehavior.class,
                "init_ajax_indicator.js", new AbstractReadOnlyModel() {
                    private static final long serialVersionUID = 1L;

                    private Map<String, Object> variables;

                    @Override
                    public Object getObject() {
                        if (variables == null) {
                            variables = new MiniMap(1);
                            variables.put("id", ajaxIndicator.getMarkupId());
                        }
                        return variables;
                    }
                }));

        super.bind(component);
    }

}
