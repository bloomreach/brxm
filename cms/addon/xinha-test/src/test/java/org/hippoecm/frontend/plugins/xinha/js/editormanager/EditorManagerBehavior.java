package org.hippoecm.frontend.plugins.xinha.js.editormanager;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.wicket.Page;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin;
import org.hippoecm.frontend.plugins.xinha.XinhaNamespace;
import org.hippoecm.frontend.plugins.xinha.js.editormanager.XinhaExtension.ListEntry;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.service.render.HeaderContributorHelper;

public class EditorManagerBehavior extends AbstractYuiBehavior {
    private static final long serialVersionUID = 1L;

    private final PackagedTextTemplate XINHA_INIT_GLOBALS = new PackagedTextTemplate(AbstractXinhaPlugin.class,
            "xinha_init.js");

    DynamicTextTemplate globals;
    DynamicTextTemplate register;

    boolean started = false;
    boolean focussed = true;

    public EditorManagerBehavior() {
        globals = new DynamicTextTemplate(XINHA_INIT_GLOBALS) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Map<String, Object> getVariables() {
                final Page page = getComponent().getPage();
                String url = HeaderContributorHelper.getFixedRelativePathPrefixToContextRoot() + "xinha/xinha/";
                String lang = page.getLocale().getLanguage();
                String skin = "hippo-lite";

                Map<String, Object> map = super.getVariables();
                map.put("editorUrl", url);
                map.put("editorLang", lang);
                map.put("editorSkin", skin);
                return map;
            }
        };
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(XinhaNamespace.NS, "editormanager");
        context.addTemplate(globals);
        context.addOnDomLoad(new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                JSONObject config = new JSONObject();
                config.put("name", getComponent().getMarkupId());
                config.put("textarea", getComponent().getMarkupId());
                config.put("started", started);
                config.put("focus", focussed);

                List<ListEntry> properties = new LinkedList<ListEntry>();
                for (IBehavior behavior : getComponent().getBehaviors()) {
                    if (behavior instanceof XinhaExtension) {
                        ((XinhaExtension) behavior).populateProperties(properties);
                    }
                }
                config.put("properties", properties);

                List<String> plugins = new LinkedList<String>();
                plugins.add("AutoSave");
                config.put("plugins", plugins);

                config.put("pluginProperties", new LinkedList());
                config.put("toolbars", new LinkedList());
                return "YAHOO.hippo.EditorManager.register(" + config.toString() + ");";
            }

        });
        context.addOnWinLoad("YAHOO.hippo.EditorManager.render();");
    }

    public void setEditorStarted(boolean b) {
        started = b;
    }

    public void setFocusAfterLoad(boolean b) {
        focussed = b;
    }
}
