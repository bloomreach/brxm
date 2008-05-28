package org.hippoecm.frontend.yui.dragdrop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.frontend.yui.logger.YuiLogBehavior;
import org.wicketstuff.yui.YuiHeaderContributor;

public abstract class AbstractDragDropBehavior extends AbstractDefaultAjaxBehavior {
    private static final long serialVersionUID = 1L;

    protected List<String> groupNames = new ArrayList<String>();
    private boolean debug;

    public AbstractDragDropBehavior() {
        super();
    }

    public AbstractDragDropBehavior(String... groupNames) {
        for (String groupName : groupNames) {
            this.groupNames.add(groupName);
        }
    }

    public IBehavior addGroup(String groupName) {
        if (!groupNames.contains(groupName)) {
            groupNames.add(groupName);
        }
        return this;
    }

    protected void clearGroups() {
        groupNames.clear();
    }

    public IBehavior setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean getDebug() {
        return debug;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        YuiHeaderContributor.forModule("dragdrop", null, debug).renderHead(response);
        super.renderHead(response);
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        onDrop(target);
    }

    @Override
    protected CharSequence getCallbackScript(boolean onlyTargetActivePage) {
        StringBuffer buf = new StringBuffer();
        buf.append("var callbackUrl = '").append(getCallbackUrl(onlyTargetActivePage)).append("';\n");
        buf.append("    for(i=0;i<callbackParameters.length;i++) {\n");
        buf
                .append("      var paramKey=callbackParameters[i].key, paramValue=Wicket.Form.encode(callbackParameters[i].value);\n");
        buf.append("      callbackUrl += (callbackUrl.indexOf('?') > -1) ? '&' : '?';\n");
        buf.append("      callbackUrl += (paramKey + '=' + paramValue);\n");
        buf.append("    }\n");
        buf.append("    ").append(generateCallbackScript("wicketAjaxGet(callbackUrl"));
        return buf.toString();
    }

    @Override
    protected void onBind() {
        final Component component = getComponent();
        if (debug)
            component.add(new YuiLogBehavior());

        component.add(TextTemplateHeaderContributor.forJavaScript(getHeaderContributorClass(),
                getHeaderContributorFilename(), getHeaderContributorVariablesModel()));
        super.onBind();
    }

    private IModel getHeaderContributorVariablesModel() {
        IModel variablesModel = new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            private Map<String, Object> variables;

            @Override
            public Object getObject() {
                if (variables == null) {
                    variables = getHeaderContributorVariables();
                }
                return variables;
            }
        };
        return variablesModel;
    }

    protected Map<String, Object> getHeaderContributorVariables() {
        final Component component = getComponent();
        Map<String, Object> variables = new MiniMap(4);
        variables.put("id", component.getMarkupId(true));
        String group = groupNames.size() > 0 ? groupNames.get(0) : "";
        variables.put("group", group);
        if (groupNames.size() > 1) {
            StringBuilder buf = new StringBuilder(16 * groupNames.size());
            buf.append('[');
            for (int i = 1; i < groupNames.size(); i++) {
                if (i > 1)
                    buf.append(',');
                buf.append('\'').append(groupNames.get(i)).append('\'');
            }
            buf.append("];");
            variables.put("moreGroups", buf.toString());
        } else {
            variables.put("moreGroups", "null");
        }
        variables.put("callbackScript", getCallbackScript());
        return variables;
    }

    protected Class<? extends IBehavior> getHeaderContributorClass() {
        return AbstractDragDropBehavior.class;
    }

    protected String getHeaderContributorFilename() {
        String className = getHeaderContributorClass().getName();
        if (className.lastIndexOf(".") > 0)
            className = className.substring(className.lastIndexOf(".") + 1);
        return className + ".js";
    }

    public abstract void onDrop(AjaxRequestTarget target);
}
