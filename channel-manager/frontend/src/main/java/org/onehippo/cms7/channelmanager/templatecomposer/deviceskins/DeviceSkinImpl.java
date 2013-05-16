/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DeviceSkinImpl implements DeviceSkin {

    private static Logger log = LoggerFactory.getLogger(DeviceSkinImpl.class);

    private static final String defaultWrapStyleTemplate = "" +
            "position: relative;\n" +
            "overflow: auto;\n" +
            "border: none;\n";

    private static final String defaultStyleTemplate = "" +
            "width: ${calc.width}px!important;\n" +
            "height: ${calc.height}px!important;\n" +
            "transform: scale(${scale.factor},${scale.factor});\n" +
            "-ms-transform: scale(${scale.factor},${scale.factor});\n" +
            "-webkit-transform: scale(${scale.factor},${scale.factor});\n" +
            "-o-transform: scale(${scale.factor},${scale.factor});\n" +
            "-moz-transform: scale(${scale.factor},${scale.factor});\n" +
            "position: absolute;\n" +
            "z-index: 10;\n" +
            "top: ${iframe.top}px;\n" +
            "left: 50%;\n" +
            "margin-left: -${iframe.left}px;\n" +
            "-moz-transform-origin: top left;\n" +
            "-webkit-transform-origin: top left;\n" +
            "-o-transform-origin: top left;\n" +
            "-ms-transform-origin: top left;\n" +
            "transform-origin: top left;\n";

    private static final String defaultIE8StyleTemplate = "" +
            "width: ${calc.width}px!important;\n" +
            "height: ${calc.height}px!important;\n" +
            "position: absolute;\n" +
            "z-index: 10;\n" +
            "top: ${iframe.top}px;\n" +
            "left: 50%;\n" +
            "margin-left: -${iframe.left}px;\n" +
            "-ms-filter: \"progid:DXImageTransform.Microsoft.Matrix(M11=${scale.factor}, M12=0, M21=0, M22=${scale.factor}, SizingMethod='auto expand')\";\n";

    private static final String defaultImgStyleTemplate = "" +
            "top: ${img.top}px;\n" +
            "margin-left: -${img.left}px;\n" +
            "left: 50%;\n" +
            "position: absolute;\n";

    private final String id;
    private final String name;

    private final Map<String,String> templateProperties = new HashMap<String,String>();

    public DeviceSkinImpl(String id, TypedResourceBundle properties) {
        this.id = id;

        this.name = properties.getString("name", "");

        // set defaults
        templateProperties.put("style", defaultStyleTemplate);
        templateProperties.put("wrapStyle", defaultWrapStyleTemplate);
        templateProperties.put("ie8Style", defaultIE8StyleTemplate);
        templateProperties.put("imgStyle", defaultImgStyleTemplate);
        templateProperties.put("image.location", "images/" + id + ".png");

        // calculate sizes
        int viewPortWidth = properties.getInteger("viewport.width", 0);
        int viewPortHeight = properties.getInteger("viewport.height", 0);
        double scaleFactor = properties.getDouble("scale.factor", 1);
        if (scaleFactor != 0.0) {
            int cw = (int) (viewPortWidth / scaleFactor);
            int ch = (int) (viewPortHeight / scaleFactor);
            templateProperties.put("calc.width", String.valueOf(cw));
            templateProperties.put("calc.height", String.valueOf(ch));
        }

        int top = properties.getInteger("top", 0);
        templateProperties.put("img.top", String.valueOf(top));
        int viewPortY = properties.getInteger("viewport.y", 0);
        templateProperties.put("iframe.top", String.valueOf(top + viewPortY));

        int backgroundWidth = properties.getInteger("background.width", 0);
        int marginLeft = backgroundWidth / 2;
        templateProperties.put("img.left", String.valueOf(marginLeft));
        int viewPortX = properties.getInteger("viewport.x", 0);
        templateProperties.put("iframe.left", String.valueOf(marginLeft - viewPortX));

        // overwrite defaults with custom values, if any
        for (String key : properties.keySet()) {
            templateProperties.put(key, properties.getString(key, ""));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ResourceReference getImage() {
        final String imageLocation = templateProperties.get("image.location");
        return new ResourceReference(getClass(), imageLocation);
    }

    @Override
    public String getCss() {
        StringBuilder buf = new StringBuilder();
        buf.append(formatCssRule(String.format(".%s > .x-panel-bwrap > .x-panel-body", id), getStyle("wrapStyle")));
        buf.append(formatCssRule(String.format(".%s > .x-panel-bwrap > .x-panel-body", id + "IE8"), getStyle("wrapStyle")));
        buf.append(formatCssRule(String.format(".%s > .x-panel-bwrap > .x-panel-body iframe", id), getStyle("style")));
        buf.append(formatCssRule(String.format(".%s > .x-panel-bwrap > .x-panel-body iframe", id + "IE8"), getStyle("ie8Style")));
        buf.append(formatCssRule(String.format(".%s > .x-panel-bwrap > .x-panel-body img", id), getStyle("imgStyle")));
        buf.append(formatCssRule(String.format(".%s > .x-panel-bwrap > .x-panel-body img", id + "IE8"), getStyle("imgStyle")));
        return buf.toString();
    }

    private String getStyle(String styleName) {
        return MapVariableInterpolator.interpolate(templateProperties.get(styleName), templateProperties);
    }

    private static final String cssRuleTemplate = "" +
            "%s{\n" +
            "%s" +
            "}\n";

    private static String formatCssRule(String selector, String declarations) {
        return String.format(cssRuleTemplate, selector, declarations);
    }

}
