/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.yui.layout;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.JsFunction;
import org.hippoecm.frontend.plugins.yui.JsFunctionProcessor;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.service.render.RenderService;

import net.sf.json.JsonConfig;

/**
 * The WireframeBehavior allows you to create cross-browser application layouts based on the YUI Layout Manager:
 * <a href="http://developer.yahoo.com/yui/layout/">http://developer.yahoo.com/yui/layout/</a> 
 * 
 * <p>
 * A layout or wireframe is a structured block containing five layout units, top,
 * right, bottom, left and center (see 
 * <a href="http://developer.yahoo.com/yui/layout/#wireframe">http://developer.yahoo.com/yui/layout/#wireframe</a>). 
 * The center unit is always present and fills up the space not occupied by it's
 * neighboring units. Units can be resized (within configurable boundaries) or have a fixed width/height and can be
 * configured to render scrollbars if needed. A wireframe can either be added to the body-element and take up the full
 * viewport, or be added to an existing element with custom width and height constraints. In both cases a resize of the
 * viewport will cause the wireframe to resize itself within the new given boundaries.
 * </p>
 * 
 * <p>
 * Wireframes can be nested as well, both render and resize events will be fired by the nearest ancestor unit, which
 * will provide the new available width and height values.
 * </p>
 * 
 * <p>
 * Every wireframe and unit corresponds with a body element in the DOM. This element is identified by it's id attribute
 * which should be known at render time and is stored, together with all other settings, inside the
 * {@link WireframeSettings} object. During the renderHead phase, this behavior will register a new wireframe with it's
 * configuration object (a JSON serialized version of the {@link WireframeSettings} on the client, which will than be
 * instantiated and rendered on either window.load or after Wicket has finished processing the Ajax response.
 * </p>
 * 
 * <p>
 * When a wireframe is rendered, it will create a new node structure inside it's body-element, representing the new
 * wireframe and it's units. It will than move the elements representing the units into their new container elements.
 * </p>
 * 
 * <p>
 * For example: (our wireframe root element is identified by id 'root' and contains two units: top[id='top'] and
 * center[id='center'])
 * <br/><br/>
 * 
 * &lt;div id="root"&gt;<br/>
 * &nbsp;&nbsp;&lt;div id="top"&gt;[ Top ]&lt;/div&gt;<br/>
 * &nbsp;&nbsp;&lt;div id="center"&gt;[ Center ]&lt;/div&gt;<br/>
 * &lt;/div&gt;
 * </p>
 * 
 * <p>
 * After the wireframe has rendered it will look something like (this is a slimmed down version of the real deal for the 
 * sake of readability)
 * <br/><br/>
 * &lt;div id="root" class="yui-layout"&gt;<br/>
 * &nbsp;&nbsp;&lt;div class="yui-layout-doc"&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;div class="yui-layout-unit yui-layout-unit-top"&gt;<br/> 
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;..<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;div id="top"&gt;[ Top ]&lt;/div&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;..<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/div&gt;<br/><br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;div class="yui-layout-unit yui-layout-unit-center"&gt;<br/> 
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;..<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;div id="center"&gt;[ Center ]&lt;/div&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;..<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/div><br/>
 * &nbsp;&nbsp;&lt;/div><br/>
 * &lt;/div><br/>
 * <p>
 * 
 * <p>
 * In the above example, the dots are actual elements added by YUI used for handling size, scrollbars, borders, etc.
 * </p>
 * 
 * <p>
 * As stated before, the id's of the wireframe's root element and all of it's units body elements, should be known at
 * render time. They are stored inside the {@link WireframeSettings} object which is serialized to JSON and registered
 * on the client.<br/> 
 * If our application only contains one single wireframe with two static unit, we can hard-code the id values into the 
 * accompanying .html file and {@link WireframeSettings}. But, as we are working in a composite environment,
 * we want our wireframes to be re-usable, without having to worry about id clashes on the client. To accompany this, we
 * created the {@link YuiId}, which allows us to do just that. See {@link YuiId} for more about that.
 * <p>
 * 
 * <p>
 * When a wireframe is nested inside another wireframe, YUI demands that the child wireframe knows the id value of it's
 * parent wireframe at render time. Because of this, a wireframe will look up it's ancestors graph for a class
 * implementing the {@link IWireframe} and, if found, retrieves and stores the parent id.
 * </p>
 * 
 * <p>
 * Another feature of the wireframe behavior is to dynamically find child components that contain a {@link UnitBehavior}
 * and register them in the {@link WireframeSettings}. This way, a {@link RenderService} can add extension points for
 * units, and know nothing about them except their position until render time.
 * </p>
 * 
 * <p>
 * To integrate the YUI layouts into Wicket, we have added a clientside manager component that handles the lifecycle, 
 * rendering and resizing of the wireframe structure.<br/> 
 * For more info see the comments in 
 * hippo-ecm-addon-yui/src/main/java/org/hippoecm/frontend/plugins/yui/inc/hippo/2.7.0/layoutmanager/layoutmanager-debug.js
 * </p>
 *
 * @see org.hippoecm.frontend.plugins.yui.layout.WireframeSettings
 * @see org.hippoecm.frontend.plugins.yui.layout.YuiId
 */

public class WireframeBehavior extends AbstractYuiAjaxBehavior implements IWireframe {
    private static final long serialVersionUID = 1L;


    private final PackagedTextTemplate behaviorJs = new PackagedTextTemplate(WireframeBehavior.class,
            "add_wireframe.js");

    private WireframeSettings settings;
    private HippoTextTemplate template;
    private boolean rendered = false;

    public WireframeBehavior(final WireframeSettings settings) {
        super(settings);
        this.settings = settings;

        template = new HippoTextTemplate(behaviorJs, settings.getClientClassName()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return settings.getRootId().getElementId();
            }

            @Override
            public Serializable getSettings() {
                return settings;
            }

            @Override
            public JsonConfig getJsonConfig() {
                JsonConfig jsonConfig = new JsonConfig();
                jsonConfig.setExcludes(new String[] {"markupId"});
                jsonConfig.registerJsonValueProcessor(YuiId.class, new YuiIdProcessor());
                jsonConfig.registerJsonValueProcessor(JsFunction.class, new JsFunctionProcessor());
                return jsonConfig;
            }
        };
    }

    public YuiId getYuiId() {
        return settings.getRootId();
    }

    @Override
    public void detach(Component component) {
        super.detach(component);
        template.detach();
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "layoutmanager");
        context.addTemplate(template);
        context.addOnDomLoad("YAHOO.hippo.LayoutManager.render()");
    }

    @Override
    public void resize(AjaxRequestTarget target) {
        if (rendered) {
            target.appendJavascript(
                "YAHOO.hippo.LayoutManager.getWireframe('" + settings.getRootId().getElementId() + "').resize()");
        }
    }

    @Override
    public void onRenderHead(IHeaderResponse response) {
        settings.setMarkupId(getComponent().getMarkupId(true));

        //If linkedWithParent, look for an ancestor Component that implements IWireframeService and retrieve it's id
        if (settings.isLinkedWithParent()) {
            Component parent = getComponent();
            boolean found = false;
            while (!found) {
                parent = parent.getParent();
                if (parent == null) {
                    throw new RuntimeException("Parent layout behavior not found for component["
                            + getComponent().getMarkupId() + "]");
                }
                for (Object parentBehavior : parent.getBehaviors()) {
                    if (parentBehavior instanceof IWireframe) {
                        IWireframe wireframe = (IWireframe) parentBehavior;
                        settings.setParentId(wireframe.getYuiId());
                        found = true;
                        break;
                    }
                }
            }
        }

        //Visit child components in order to find components that contain a {@link UnitBehavior}. If another wireframe
        //or unit is encountered, stop going deeper.
        MarkupContainer cont = (MarkupContainer) getComponent();
        cont.visitChildren(new IVisitor<Component>() {
            public Object component(Component component) {
                for (Object behavior : component.getBehaviors()) {
                    if (behavior instanceof IWireframe) {
                        return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                    } else if (behavior instanceof UnitBehavior) {
                        String position = ((UnitBehavior) behavior).getPosition();
                        UnitSettings unit = settings.getUnit(position);
                        if (unit != null) {
                            YuiId body = unit.getBody();
                            if (body != null) {
                                body.setParentId(null);
                                body.setId(component.getMarkupId());
                            }
                        } else {
                            throw new RuntimeException("Invalid UnitBehavior position " + position);
                        }
                        return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                    }
                }
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });

        super.onRenderHead(response);

        rendered  = true;
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        final RequestCycle requestCycle = RequestCycle.get();
        String position = requestCycle.getRequest().getParameter("position");
        if (!Strings.isEmpty(position)) {
            onToggleFromClient(position, toggle(position));
        }
    }

    protected void onToggleFromClient(String position, boolean expand) {
    }

    @Deprecated
    public boolean toggle(String position, AjaxRequestTarget target) {
        return toggle(position);
    }

    public boolean toggle(String position) {
        UnitSettings unitSettings = settings.getUnit(position);
        if (unitSettings == null) {
            throw new IllegalArgumentException(
                    "No unit with position " + position + " is defined in layout[" + settings.getRootId() + "], cannot expand/collapse.");
        }

        boolean expand = !unitSettings.isExpanded();
        AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target != null) {
            String jsMethod = expand ? "YAHOO.hippo.LayoutManager.expandUnit" : "YAHOO.hippo.LayoutManager.collapseUnit";
            target.appendJavascript(
                    jsMethod + "('" + this.settings.getRootId().getElementId() + "', '" + position + "');");
        }
        unitSettings.setExpanded(expand);
        onToggle(expand, position);
        return expand;
    }

    protected void onToggle(boolean expand, String position) {
    }

    public void collapseAll() {
        for (UnitSettings unit : settings.getUnits()) {
            if (unit.isExpanded()) {
                toggle(unit.getPosition());
            }
        }
    }

    public void expandDefault() {
        String defaultExpandedUnit = settings.getDefaultExpandedUnit();
        if(defaultExpandedUnit != null) {
            UnitSettings defaultSettings = settings.getUnit(defaultExpandedUnit);
            if(defaultSettings == null) {
                return;
            }
            if (defaultSettings.isExpandCollapseEnabled()) {
                if(!settings.hasExpandedUnit()) {
                    AjaxRequestTarget target = AjaxRequestTarget.get();
                    if(target != null) {
                        String jsMethod = "YAHOO.hippo.LayoutManager.expandUnit" + "('" + this.settings.getRootId().getElementId() + "', '" + defaultSettings.getPosition() + "');";
                        target.appendJavascript(jsMethod);
                    }
                    defaultSettings.setExpanded(true);
                }
                onExpandDefault();
            }
        }
    }

    protected void onExpandDefault() {
    }

    public boolean hasExpandableUnit() {
        for (UnitSettings unit : settings.getUnits()) {
            if (unit.isExpandCollapseEnabled()) {
                return true;
            }
        }
        return false;
    }
}
