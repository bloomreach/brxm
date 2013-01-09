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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.internal.HeaderResponse;
import org.apache.wicket.response.StringResponse;
import org.apache.wicket.util.string.JavascriptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of Wicket's {@link AjaxRequestTarget} that filters the list of
 * {@link Component}s that have been added.
 * <p>
 * This implementation of the AjaxRequestTarget allows components to be
 * added to the request target even when they are later on removed from the
 * (visible) component tree.  The default wicket way requires that the
 * component that responds to an ajax request knows which components need to
 * be redrawn and which changes are made in the component hierarchy.
 * <p>
 * For the CMS plugin framework, this coupling has been lifted using this
 * extension, the PluginRequestTarget.  In the processing of ajax requests,
 * the action phase (invocation of the listener) marks components that need
 * to be redrawn (see RenderService#redraw), that later on can actually
 * register for rendering (IRenderService#render).
 * <p>
 * For regular wicket components, this is not an option.  So for those are
 * still able to rerender themselves after e.g. a model change.  Since other
 * plugins might change the hierarchy in response to the changes, the
 * component might no longer be there during the rendering though.  The
 * PluginRequestTarget handles this case by discarding the component.
 */
public class PluginRequestTarget extends AjaxRequestTarget implements AjaxRequestTarget.IListener {

    private static final Logger log = LoggerFactory.getLogger(PluginRequestTarget.class);

    private Set<Component> updates;
    private List<IListener> listeners;

    private final List<String> appendJavascripts = new ArrayList<String>();
    private final List<String> domReadyJavascripts = new ArrayList<String>();

    public PluginRequestTarget(Page page) {
        super(page);

        this.updates = new HashSet<Component>();

        super.addListener(this);
    }

    @Override
    public void addComponent(Component component) {
        if (component == null) {
            throw new IllegalArgumentException("component cannot be null");
        }
        if (component.getOutputMarkupId() == false) {
            throw new IllegalArgumentException("cannot update component that does not have setOutputMarkupId property set to true. Component: " + component.toString());
        }
        if (component.findParent(Page.class) != getPage()) {
            log.debug("ignoring component {} belonging to outdated page version", component.getId());
            return;
        }
        this.updates.add(component);
    }

    @Override
    public void addListener(IListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Argument `listener` cannot be null");
        }

        if (listeners == null) {
            listeners = new LinkedList();
        }

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // implement AjaxRequestTarget.IListener

    public void onBeforeRespond(Map existing, AjaxRequestTarget target) {
        if (existing.size() > 0) {
            log.warn("Some components have already been added to the target.");
        }

        if (listeners != null) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                ((IListener) it.next()).onBeforeRespond(existing, this);
            }
        }

        TreeMap<String, Component> toUpdate = new TreeMap<String, Component>();
        Iterator<Component> components = updates.iterator();
        while (components.hasNext()) {
            Component component = components.next();
            if (!component.isVisibleInHierarchy()) {
                continue;
            }

            MarkupContainer parent = component.getParent();
            while (parent != null) {
                if (parent instanceof Page) {
                    toUpdate.put(component.getPath(), component);
                    break;
                } else if (updates.contains(parent)) {
                    break;
                }
                parent = parent.getParent();
            }
        }
        for (Component component : toUpdate.values()) {
            super.addComponent(component);
        }
    }

    public void onAfterRespond(Map map, IJavascriptResponse response) {
        if (listeners != null) {
            Iterator<IListener> it = listeners.iterator();
            while (it.hasNext()) {
                it.next().onAfterRespond(map, response);
            }
        }

        // execute the dom ready javascripts as first javascripts
        // after component replacement
        Response webResponse = RequestCycle.get().getResponse();
        Iterator<String> it = domReadyJavascripts.iterator();
        while (it.hasNext()) {
            String js = it.next();
            respondInvocation(webResponse, js);
        }
        it = appendJavascripts.iterator();
        while (it.hasNext()) {
            String js = it.next();
            respondInvocation(webResponse, js);
        }
    }

    private void respondInvocation(final Response response, final String js) {
        boolean encoded = false;
        String javascript = js;

        // encode the response if needed
        if (needsEncoding(js)) {
            encoded = true;
            javascript = encode(js);
        }

        response.write("<evaluate");
        if (encoded) {
            response.write(" encoding=\"");
            response.write(getEncodingName());
            response.write("\"");
        }
        response.write(">");
        response.write("<![CDATA[");
        response.write(javascript);
        response.write("]]>");
        response.write("</evaluate>");
    }

    @Override
    public IHeaderResponse getHeaderResponse() {
        return new HeaderResponse() {
            private final StringResponse bufferedResponse = new StringResponse();

            @Override
            public void close() {
                super.close();

                Response realResponse = RequestCycle.get().getResponse();
                CharSequence output = bufferedResponse.getBuffer();
                if (output.length() > 0) {
                    realResponse.write(output);
                }
                bufferedResponse.reset();
            }

            @Override
            public void renderJavascript(final CharSequence javascript, final String id) {
                if (javascript == null) {
                    throw new IllegalArgumentException("javascript cannot be null");
                }
                List<Object> token = Arrays.asList(new Object[]{javascript.toString(), id});
                if (wasRendered(token) == false) {
                    JavascriptUtils.writeJavascript(RequestCycle.get().getResponse(), javascript, id);
                    markRendered(token);
                }
            }

            @Override
            public void renderString(final CharSequence string) {
                if (string == null) {
                    throw new IllegalArgumentException("string cannot be null");
                }
                String token = string.toString();
                if (wasRendered(token) == false)
                {
                    RequestCycle.get().getResponse().write(string);
                    markRendered(token);
                }
            }

            @Override
            public void renderOnDomReadyJavascript(String javascript) {
                List<String> token = Arrays.asList(new String[]{"javascript-event", "window", "domready", javascript});
                if (wasRendered(token) == false) {
                    domReadyJavascripts.add(javascript);
                    markRendered(token);
                }
            }

            @Override
            public void renderOnLoadJavascript(String javascript) {
                List<String> token = Arrays.asList(new String[]{"javascript-event", "window", "load", javascript});
                if (wasRendered(token) == false) {
                    // execute the javascript after all other scripts are executed
                    appendJavascripts.add(javascript);
                    markRendered(token);
                }
            }

            @Override
            protected Response getRealResponse() {
                return bufferedResponse;
            }
        };
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

}
