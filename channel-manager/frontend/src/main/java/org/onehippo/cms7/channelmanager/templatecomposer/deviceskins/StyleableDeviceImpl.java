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
import java.util.ResourceBundle;

import org.apache.wicket.Session;
import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class StyleableDeviceImpl implements StyleableDevice {

    private static Logger log = LoggerFactory.getLogger(StyleableDeviceImpl.class);

    private static final String defaultWrapStyleTemplate = "" +
            "background: url('${image.location}') 0 0 no-repeat;\n" +
            "width: ${background.width}px;\n" +
            "height:${background.height}px;\n" +
            "background-position: 0px 0px;\n" +
            "background-repeat: no-repeat no-repeat;\n" +
            "position: relative;\n" +
            "border: none;";

    private static final String defaultStyleTemplate = "" +
            "width: ${calc.width}px!important;\n" +
            "height: ${calc.height}px!important;\n" +
            "transform: scale(${scale.factor},${scale.factor});\n" +
            "-ms-transform: scale(${scale.factor},${scale.factor});\n" +
            "-webkit-transform: scale(${scale.factor},${scale.factor});\n" +
            "-o-transform: scale(${scale.factor},${scale.factor});\n" +
            "-moz-transform: scale(${scale.factor},${scale.factor});\n" +
            "transform: scale(${scale.factor},${scale.factor});\n" +
            "position: absolute;\n" +
            "top: ${viewport.y}px;\n" +
            "left: ${viewport.x}px;\n" +
            "-moz-transform-origin: top left;\n" +
            "-webkit-transform-origin: top left;\n" +
            "-o-transform-origin: top left;\n" +
            "-ms-transform-origin: top left;\n" +
            "transform-origin: top left;";

    private final String id;
    private final String name;

    private final Map<String,String> templateProperties = new HashMap<String,String>();

    public StyleableDeviceImpl(String id) {
        this.id = id;
        ResourceBundle properties = ResourceBundle.getBundle(
                this.getClass().getPackage().getName() + ".devices." + id,
                Session.get().getLocale()
        );
        this.name = properties.getString("name");
        templateProperties.put("style", defaultStyleTemplate);
        templateProperties.put("wrapStyle", defaultWrapStyleTemplate);
        templateProperties.put("image.location", "images/" + id + ".png");
        for (String property : properties.keySet()) {
            templateProperties.put(property, properties.getString(property));
        }
        if (properties.containsKey("autoCalc") && "true".equals(properties.getString("autoCalc"))) {
            autoCalcSize();
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
    public String getStyle() {
        return processTemplate(templateProperties.get("style"));
    }

    @Override
    public String getWrapStyle() {
        return processTemplate(templateProperties.get("wrapStyle"));
    }

    private void autoCalcSize() {
        try {
            int viewPortWidth = Integer.parseInt(templateProperties.get("viewport.width"));
            int viewPortHeight = Integer.parseInt(templateProperties.get("viewport.height"));
            double scaleFactor = Double.parseDouble(templateProperties.get("scale.factor"));
            if (scaleFactor != 0.0) {
                int cw = (int) Math.floor(viewPortWidth / scaleFactor);
                int ch = (int) Math.floor(viewPortHeight / scaleFactor);
                templateProperties.put("calc.width", String.valueOf(cw));
                templateProperties.put("calc.height", String.valueOf(ch));
            }
        } catch (NumberFormatException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String processTemplate(String style) {
        return MapVariableInterpolator.interpolate(style, templateProperties);
    }
}
