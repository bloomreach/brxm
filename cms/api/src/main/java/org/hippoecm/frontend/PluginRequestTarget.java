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
package org.hippoecm.frontend;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.ILogData;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Extension of Wicket's {@link AjaxRequestTarget} that filters the list of
 * {@link Component}s that have been added.
 * <p>
 * This class used to handle the case that nested components were added to the request target.
 * While necessary in Wicket-1.4.x, in Wicket-6, this is handled by the framework.
 */
public final class PluginRequestTarget implements AjaxRequestTarget {

    private final AjaxRequestTarget upstream;
    private final OnlyRenderComponentsOnPage componentsToRender;

    public PluginRequestTarget(final AjaxRequestTarget upstream) {
        this.upstream = upstream;
        this.componentsToRender = new OnlyRenderComponentsOnPage(upstream.getPage());
        registerRespondListener(this.componentsToRender);
    }

    @Override
    public void add(final Component component, final String markupId) {
        upstream.add(component, markupId);
    }

    /**
     * Adds a component to this Ajax request, but only if it is still part of the page when this Ajax request
     * begins to respond.
     *
     * @param components the components to add
     */
    @Override
    public void add(final Component... components) {
        componentsToRender.add(components);
    }

    @Override
    public void addChildren(final MarkupContainer parent, final Class<?> childCriteria) {
        upstream.addChildren(parent, childCriteria);
    }

    @Override
    public void addListener(final IListener listener) {
        upstream.addListener(listener);
    }

    @Override
    public void appendJavaScript(final CharSequence javascript) {
        upstream.appendJavaScript(javascript);
    }

    @Override
    public void prependJavaScript(final CharSequence javascript) {
        upstream.prependJavaScript(javascript);
    }

    @Override
    public void registerRespondListener(final ITargetRespondListener listener) {
        upstream.registerRespondListener(listener);
    }

    @Override
    public Collection<? extends Component> getComponents() {
        return upstream.getComponents();
    }

    @Override
    public void focusComponent(final Component component) {
        upstream.focusComponent(component);
    }

    @Override
    public IHeaderResponse getHeaderResponse() {
        return upstream.getHeaderResponse();
    }

    @Override
    public String getLastFocusedElementId() {
        return upstream.getLastFocusedElementId();
    }

    @Override
    public Page getPage() {
        return upstream.getPage();
    }

    @Override
    public Integer getPageId() {
        return upstream.getPageId();
    }

    @Override
    public boolean isPageInstanceCreated() {
        return upstream.isPageInstanceCreated();
    }

    @Override
    public Integer getRenderCount() {
        return upstream.getRenderCount();
    }

    @Override
    public Class<? extends IRequestablePage> getPageClass() {
        return upstream.getPageClass();
    }

    @Override
    public PageParameters getPageParameters() {
        return upstream.getPageParameters();
    }

    @Override
    public void respond(final IRequestCycle requestCycle) {
        IRequestablePage page = upstream.getPage();
        if ((page instanceof Home)) {
            final Home home = (Home) page;
            home.processEvents();
            home.render(this);
        }
        upstream.respond(requestCycle);
    }

    @Override
    public void detach(final IRequestCycle requestCycle) {
        upstream.detach(requestCycle);
    }

    @Override
    public ILogData getLogData() {
        return upstream.getLogData();
    }

    @Override
    public int hashCode() {
        return 324889 ^ super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PluginRequestTarget) {
            return super.equals(obj);
        }
        return false;
    }

    private static class OnlyRenderComponentsOnPage implements ITargetRespondListener {

        private final Set<Component> components;
        private final Page upstreamPage;

        public OnlyRenderComponentsOnPage(Page upstreamPage) {
            this.components = new TreeSet<>(Comparator.comparing(Component::getMarkupId));
            this.upstreamPage = upstreamPage;
        }

        void add(Component... components) {
            this.components.addAll(Arrays.asList(components));
        }

        @Override
        public void onTargetRespond(final AjaxRequestTarget target) {
            // Only add the components that have upstreamPage as parent.
            final Component[] componentArray = this.components.stream()
                    .filter(component -> upstreamPage.equals(component.findParent(Page.class)))
                    .toArray(Component[]::new);
            target.add(componentArray);
        }

    }
}
