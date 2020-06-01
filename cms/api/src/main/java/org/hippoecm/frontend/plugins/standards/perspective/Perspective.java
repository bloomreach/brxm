/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.perspective;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.PackageResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.usagestatistics.UsageEvent;
import org.hippoecm.frontend.usagestatistics.UsageStatisticsHeaderItem;

public abstract class Perspective extends RenderPlugin<Void> implements ITitleDecorator {

    // deprecated
    @Deprecated
    public static final String TITLE = "perspective.title";
    public static final String TITLE_KEY = "perspective-title";

    public static final String CLUSTER_NAME = "cluster.name";
    public static final String CLUSTER_PARAMETERS = "cluster.config";

    public static final String IMAGE_EXTENSION = "svg";
    public static final String FALLBACK_IMAGE_EXTENSION = "png";

    private static final String EVENT_PERSPECTIVE_ACTIVATED = "perspective-activated";
    private static final String EVENT_PARAM_PERSPECTIVE_ID = "perspectiveId";
    private static final String EVENT_PARAM_CMS = "CMS";

    private static final ArrayList<String> cmsEventNamesList;
    static {
        cmsEventNamesList = new ArrayList<>();
        cmsEventNamesList.add("dashboard");
        cmsEventNamesList.add("channels");
        cmsEventNamesList.add("content");
        cmsEventNamesList.add("reports");
        cmsEventNamesList.add("admin");
        cmsEventNamesList.add("formdata");
        cmsEventNamesList.add("audiences");
        cmsEventNamesList.add("search");
        cmsEventNamesList.add("login");
        cmsEventNamesList.add("logout");
        cmsEventNamesList.add("projects");
    }

    private final String eventId;
    private final IModel<String> title;

    private final String imageExtension;
    private final String fallbackImageExtension;

    private boolean isRendered;
    private boolean isActivated;

    public Perspective(IPluginContext context, IPluginConfig config) {
        this(context, config, null);
    }

    public Perspective(IPluginContext context, IPluginConfig config, final String eventId) {
        super(context, config);

        this.eventId = eventId;

        String titleKey = config.getString(TITLE);
        if (titleKey != null) {
            log.warn("Property {} on perspective configuration of perspective {} is deprecated, just use key 'perspective-title' " +
                    "in the perspective's java resource bundle", TITLE, getClass());
        } else {
            titleKey = TITLE_KEY;
        }
        this.title = new StringResourceModel(titleKey, this, null);

        imageExtension = config.getString("image.extension", IMAGE_EXTENSION);
        fallbackImageExtension = config.getString("fallback.image.extension", FALLBACK_IMAGE_EXTENSION);

        add(CssClass.append("perspective"));
    }

    public String getTitleCssClass() {
        // return a stable CSS class name to be able to identify a perspective's link in automated tests
        return "hippo-perspective-" + getClass().getSimpleName().toLowerCase();
    }

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    public ResourceReference getIcon(IconSize size) {
        // try (name)-(size).svg
        String image = toImageName(getClass().getSimpleName(), size, imageExtension);
        if (PackageResource.exists(getClass(), image, null, null, null)) {
            return new PackageResourceReference(getClass(), image);
        }

        // try (name).svg
        image = toImageName(getClass().getSimpleName(), null, imageExtension);
        if (PackageResource.exists(getClass(), image, null, null, null)) {
            return new PackageResourceReference(getClass(), image);
        }

        // try (name)-(size).png
        image = toImageName(getClass().getSimpleName(), size, fallbackImageExtension);
        if (PackageResource.exists(getClass(), image, null, null, null)) {
            return new PackageResourceReference(getClass(), image);
        }

        // use built-in picture
        image = toImageName(Perspective.class.getSimpleName(), size, fallbackImageExtension);
        if (PackageResource.exists(Perspective.class, image, null, null, null)) {
            return new PackageResourceReference(Perspective.class, image);
        }

        return null;
    }

    protected String toImageName(final String camelCaseString, final IconSize size, final String extension) {
        StringBuilder name = new StringBuilder(camelCaseString.length());
        name.append(Character.toLowerCase(camelCaseString.charAt(0)));
        for (int i = 1; i < camelCaseString.length(); i++) {
            char c = camelCaseString.charAt(i);
            if (Character.isUpperCase(c)) {
                name.append('-').append(Character.toLowerCase(c));
            } else {
                name.append(c);
            }
        }
        if (size != null) {
            name.append('-').append(size.getSize());
        }
        name.append('.').append(extension);

        return name.toString();
    }

    protected void setTitle(String title) {
        this.title.setObject(title);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        if (eventsEnabled() && isActive()) {
            isActivated = true;
            response.render(new PerspectiveActivatedHeaderItem(eventId));
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (isActive()) {
            if (!isRendered) {
                isRendered = true;
                startPluginCluster();
            }

            if (!isActivated) {
                isActivated = true;
                onActivated();
            }
        } else {
            if (isActivated) {
                isActivated = false;
                onDeactivated();
            }
        }
        super.render(target);
    }

    /**
     * Hook called when the perspective is activated, i.e. transitions from inactive to active state.
     * When overiding, make sure to call super.onActivated() in order to keep the usage statistics working.
     */
    protected void onActivated() {
        if (StringUtils.isNotEmpty(eventId) && cmsEventNamesList.contains(eventId)) {
            final String event = EVENT_PARAM_CMS + StringUtils.capitalize(eventId);
            publishEvent(event);
        }
    }

    /**
     * Hook called when the perspective is deactivate, i.e. transitions from active to inactive state.
     * When overriding, make sure to call super.onDeactivated().
     */
    protected void onDeactivated() {
    }

    protected void publishEvent(final String name) {
        if (eventsEnabled() && StringUtils.isNotEmpty(name)) {
            final UsageEvent event = new UsageEvent(name);
            event.publish();
        }
    }

    private boolean eventsEnabled() {
        return eventId != null;
    }

    private void startPluginCluster() {
        final IPluginConfig config = getPluginConfig();
        final String clusterName = config.getString(CLUSTER_NAME);
        if (clusterName != null) {
            final IPluginContext context = getPluginContext();
            final IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(), IPluginConfigService.class);

            final IClusterConfig cluster = pluginConfigService.getCluster(clusterName);
            if (cluster == null) {
                log.warn("Unable to find cluster '" + clusterName + "'. Does it exist in repository?");
            } else {
                final IPluginConfig parameters = config.getPluginConfig(CLUSTER_PARAMETERS);
                final IClusterControl control = context.newCluster(cluster, parameters);
                control.start();
            }
        }
    }

    private static class PerspectiveActivatedHeaderItem extends HeaderItem {

        private final String perspectiveId;

        PerspectiveActivatedHeaderItem(String perspectiveId) {
            this.perspectiveId = perspectiveId;
        }

        @Override
        public Iterable<?> getRenderTokens() {
            return Collections.singleton("perspective-activated-header-item");
        }

        @Override
        public Iterable<? extends HeaderItem> getDependencies() {
            return Collections.singleton(UsageStatisticsHeaderItem.get());
        }

        @Override
        public void render(final Response response) {
            if(StringUtils.isNotEmpty(perspectiveId)) {
                final UsageEvent perspectiveActivated = new UsageEvent(perspectiveId);

                final String eventJs = perspectiveActivated.getJavaScript();
                OnLoadHeaderItem.forScript(eventJs).render(response);
            }
        }
    }

}
