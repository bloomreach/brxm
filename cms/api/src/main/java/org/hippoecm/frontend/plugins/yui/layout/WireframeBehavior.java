/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.JsFunction;
import org.hippoecm.frontend.plugins.yui.JsFunctionProcessor;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.service.render.RenderService;

import net.sf.json.JsonConfig;

/**
 * <p> The WireframeBehavior allows you to create cross-browser application layouts based on the
 * <a href="http://yui.github.io/yui2/docs/yui_2.9.0_full/layout/index.html">YUI Layout Manager</a>. A layout or
 * wireframe is a structured block containing five layout units: top, right, bottom, left and center. The center unit
 * is always present and fills up the space not occupied by it's neighboring units. Units can be resized (within
 * configurable boundaries) or have a fixed width/height and can be configured to render scrollbars if needed. A
 * wireframe can either be added to the body-element and take up the full viewport, or be added to an existing element
 * with custom width and height constraints. In both cases a resize of the viewport will cause the wireframe to resize
 * itself within the new given boundaries. </p>
 *
 * <p> Wireframes can be nested as well, both render and resize events will be fired by the nearest ancestor unit,
 * which will provide the new available width and height values. </p>
 *
 * <p> Every wireframe and unit corresponds with a body element in the DOM. This element is identified by it's id
 * attribute which should be known at render time and is stored, together with all other settings, inside the {@link
 * WireframeSettings} object. During the renderHead phase, this behavior will register a new wireframe with it's
 * configuration object on the client using a JSON serialized version of the {@link WireframeSettings}, which will than be
 * instantiated and rendered on either window.load or after Wicket has finished processing the Ajax response.</p>
 *
 * <p> When a wireframe is rendered, it will create a new node structure inside it's body-element. It will than move the
 * elements representing the units into their new container elements. For example, our wireframe's root element is
 * identified by id 'root' and contains two units: top and center.<br/>
 * After the wireframe has rendered it will look something like (this is a slimmed down version of the real deal for the
 * sake of readability) <br/><br/> &lt;div id="root" class="yui-layout"&gt;<br/> &nbsp;&nbsp;&lt;div
 * class="yui-layout-doc"&gt;<br/> &nbsp;&nbsp;&nbsp;&nbsp;&lt;div class="yui-layout-unit yui-layout-unit-top"&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;..<br/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;div id="top"&gt;[ Top
 * ]&lt;/div&gt;<br/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;..<br/> &nbsp;&nbsp;&nbsp;&nbsp;&lt;/div&gt;<br/><br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;div class="yui-layout-unit yui-layout-unit-center"&gt;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;..<br/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;div id="center"&gt;[
 * Center ]&lt;/div&gt;<br/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;..<br/> &nbsp;&nbsp;&nbsp;&nbsp;&lt;/div><br/>
 * &nbsp;&nbsp;&lt;/div><br/> &lt;/div></p>
 *
 * <p> As stated before, the id's of the wireframe's root element and all of it's units body elements, should be known
 * at render time. If our application only contains one single wireframe with two static unit, we can hard-code the id
 * values into the accompanying .html file and {@link WireframeSettings}. But, as we are working in a composite
 * environment, we want our wireframes to be re-usable, without having to worry about id clashes on the client. To
 * accompany this, we created the {@link YuiId}, which allows us to do just that. See {@link YuiId} for more about that.</p>
 *
 * <p> When a wireframe is nested inside another wireframe, YUI demands that the child wireframe knows the id value of
 * it's parent wireframe at render time. Because of this, a wireframe will look up it's ancestors graph for a class
 * implementing the {@link IWireframe} and, if found, retrieve and store the parent id. </p>
 *
 * <p> Another feature of the wireframe behavior is to dynamically find child components that contain a
 * {@link UnitBehavior} and register them in the {@link WireframeSettings}. This way, a {@link RenderService} can add
 * extension points for units, and know nothing about them except their position until render time. </p>
 *
 * <p> To integrate the YUI layouts into Wicket, we have added a clientside manager component that handles the lifecycle,
 * rendering and resizing behavior of the wireframes. For more info see {@link /api/src/main/java/org/hippoecm/frontend/plugins/yui/inc/hippo/281/layoutmanager/layoutmanager-debug.js}
 * </p>
 *
 * @see org.hippoecm.frontend.plugins.yui.layout.WireframeSettings
 * @see org.hippoecm.frontend.plugins.yui.layout.YuiId
 */

public class WireframeBehavior extends AbstractYuiAjaxBehavior implements IWireframe {

    private final PackageTextTemplate ADD_WIREFRAME_TEMPLATE = new PackageTextTemplate(WireframeBehavior.class,
            "add_wireframe.js");

    private final WireframeSettings settings;
    private final HippoTextTemplate template;
    private boolean rendered = false;

    public WireframeBehavior(final WireframeSettings settings) {
        super(settings);
        this.settings = settings;

        template = new HippoTextTemplate(ADD_WIREFRAME_TEMPLATE, settings.getClientClassName()) {

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
                jsonConfig.setExcludes(new String[]{"markupId"});
                jsonConfig.registerJsonValueProcessor(YuiId.class, new YuiIdProcessor());
                jsonConfig.registerJsonValueProcessor(JsFunction.class, new JsFunctionProcessor());
                return jsonConfig;
            }
        };
    }

    public YuiId getYuiId() {
        final YuiId rootId = settings.getRootId();
        if (rootId.getParentId() == null) {
            settings.setMarkupId(getComponent().getMarkupId());
        }
        return rootId;
    }

    @Override
    public boolean isRendered() {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            Component parent = getComponent();
            while (parent != null && !target.getComponents().contains(parent)) {
                parent = parent.getParent();
            }
            if (parent != null) {
                return false;
            }
        } else {
            return false;
        }
        return rendered;
    }

    @Override
    public void render(final AjaxRequestTarget target) {
        rendered = false;
        target.add(getComponent());
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "layoutmanager");
    }

    @Override
    protected void onRenderHead(final IHeaderResponse response) {
        if (isRendered()) {
            return;
        }

        final String markupId = getComponent().getMarkupId(true);

        updateAjaxSettings();

        settings.setMarkupId(markupId);

        IWireframe parentWireframe = getParentWireframe();
        if (parentWireframe != null) {
            settings.setParentId(parentWireframe.getYuiId());
        }

        //Visit child components in order to find components that contain a {@link UnitBehavior}. If another wireframe
        //or unit is encountered, stop going deeper.
        MarkupContainer cont = (MarkupContainer) getComponent();
        cont.visitChildren(new IVisitor<Component, Void>() {
            public void component(Component component, IVisit<Void> visit) {
                for (Object behavior : component.getBehaviors()) {
                    if (behavior instanceof IWireframe) {
                        visit.dontGoDeeper();
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
                        visit.dontGoDeeper();
                    }
                }
            }
        });

        rendered = true;

        response.render(getHeaderItem());
    }

    @Override
    public void resize(AjaxRequestTarget target) {
        if (rendered) {
            target.appendJavaScript(
                    "YAHOO.hippo.LayoutManager.getWireframe('" + settings.getRootId().getElementId() + "').resize()");
        }
    }

    protected IWireframe getParentWireframe() {
        final String markupId = getComponent().getMarkupId(true);

        //If linkedWithParent, look for an ancestor Component that implements IWireframeService and retrieve it's id
        if (settings.isLinkedWithParent()) {
            IWireframe parent = WireframeUtils.getParentWireframe(getComponent());
            if (parent != null) {
                return parent;
            }
            throw new RuntimeException("Parent layout behavior not found for component[" + markupId + "]");
        }

        return null;
    }

    public HeaderItem getHeaderItem() {
        return new OnDomReadyHeaderItem("not-empty") {

            private String getId() {
                return getComponent().getMarkupId(true) + "-wireframe-behavior";
            }

            @Override
            public Iterable<?> getRenderTokens() {
                return Collections.singletonList(getId());
            }

            @Override
            public List<HeaderItem> getDependencies() {
                IWireframe wireframe = getParentWireframe();
                if (wireframe != null && !wireframe.isRendered()) {
                    return Collections.singletonList(wireframe.getHeaderItem());
                }
                return super.getDependencies();
            }

            @Override
            public int hashCode() {
                return getId().hashCode();
            }

            @Override
            public boolean equals(final Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj instanceof OnDomReadyHeaderItem) {
                    return getRenderTokens().equals(((OnDomReadyHeaderItem) obj).getRenderTokens());
                }
                return false;
            }

            @Override
            public CharSequence getJavaScript() {
                return template.getString();
            }

        };
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        final RequestCycle requestCycle = RequestCycle.get();
        StringValue position = requestCycle.getRequest().getRequestParameters().getParameterValue("position");
        if (!position.isNull()) {
            final String strPos = position.toString();
            if (!Strings.isEmpty(strPos)) {
                onToggleFromClient(strPos, toggle(strPos));
            }
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
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            String jsMethod = expand ? "YAHOO.hippo.LayoutManager.expandUnit" : "YAHOO.hippo.LayoutManager.collapseUnit";
            target.appendJavaScript(
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

    /**
     * If no unit has been expanded and a default expanded unit has been configured, it will be expanded.
     */
    public void expandDefault() {
        final String defaultExpandedUnit = settings.getDefaultExpandedUnit();
        if (defaultExpandedUnit == null) {
            return;
        }

        final UnitSettings defaultExpandedUnitSettings = settings.getUnit(defaultExpandedUnit);
        if (defaultExpandedUnitSettings == null || !defaultExpandedUnitSettings.isExpandCollapseEnabled()) {
            return;
        }

        if (!settings.hasExpandedUnit()) {
            final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                final String jsMethod = String.format("YAHOO.hippo.LayoutManager.expandUnit('%s', '%s');",
                        settings.getRootId().getElementId(), defaultExpandedUnitSettings.getPosition());
                target.appendJavaScript(jsMethod);
            }
            defaultExpandedUnitSettings.setExpanded(true);
        }
        onExpandDefault();
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
