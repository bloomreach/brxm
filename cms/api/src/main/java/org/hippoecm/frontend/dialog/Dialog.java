/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.dialog;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.core.request.ClientInfo;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;

public class Dialog<T> extends AbstractDialog<T> {

    public static final String BOTTOM_LEFT_ID = "bottom-left";

    private IValueMap size = DialogConstants.LARGE_AUTO;
    private IValueMap properties;

    private String cssClass;
    private String titleKey = "dialog.title";
    private IModel<?> titleModel;

    public Dialog() {
        this(null);
    }

    public Dialog(final IModel<T> model) {
        super(model);

        setButtonCssClass("hippo-button");

        add(new EmptyPanel(BOTTOM_LEFT_ID).setVisible(false));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        ClientInfo info = Session.get().getClientInfo();
        if (info instanceof WebClientInfo) {
            WebClientInfo webInfo = (WebClientInfo) info;
            // IE10 needs a width in pixels on the .hippo-dialog.bottom-left panel in order to render
            // items that have overflow:hidden correctly.
            if (webInfo.getProperties().isBrowserInternetExplorer() &&
                    webInfo.getProperties().getBrowserVersionMajor() == 10) {
                final String script = String.format("$('%s').width($('%s').width() - $('%s').width())",
                        ".hippo-dialog-bottom-left", ".hippo-dialog-bottom", ".hippo-dialog-bottom-right");

                response.render(OnLoadHeaderItem.forScript(script));
            }
        }
        super.renderHead(response);
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of(getString(titleKey, titleModel, StringUtils.EMPTY));
    }

    @Override
    public IValueMap getProperties() {
        if (properties == null) {
            if (StringUtils.isNotEmpty(cssClass)) {
                Map<String, Object> map = new LinkedHashMap<>(size);
                map.put("css-class-name", cssClass);
                properties = new ValueMap(map).makeImmutable();
            } else {
                properties = size;
            }
        }
        return properties;
    }

    @Override
    protected boolean addAjaxIndicator() {
        return false;
    }

    protected void setTitleKey(final String titleKey) {
        this.titleKey = titleKey;
    }

    protected void setTitleModel(IModel<?> titleModel) {
        this.titleModel = titleModel;
    }

    protected void setSize(final IValueMap size) {
        this.size = size;
    }

    protected void setCssClass(final String cssClass) {
        this.cssClass = cssClass;
    }
}
