package org.hippoecm.frontend.usagestatistics;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.cycle.RequestCycle;

import net.sf.json.JSONObject;

public class UsageEvent {

    private final String name;
    private final JSONObject parameters;

    public UsageEvent(final String name) {
        this.name = name;
        this.parameters = new JSONObject();
    }

    public void setParameter(final String name, final String value) {
        parameters.put(name, value);
    }

    public void publish() {
        final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.appendJavaScript(getJavaScript());
        }
    }

    public String getJavaScript() {
        final StringBuilder js = new StringBuilder("Hippo.Events.publish('");
        js.append(name);
        js.append("'");
        if (!parameters.isEmpty()) {
            js.append(",");
            js.append(parameters.toString());
        }
        js.append(");");
        return js.toString();
    }

}
