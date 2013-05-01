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

    public static final String BACKGROUND_WIDTH = "background.width";
    public static final String BACKGROUND_HEIGHT = "background.height";
    public static final String VIEWPORT_X = "viewport.x";
    public static final String VIEWPORT_Y = "viewport.y";
    public static final String VIEWPORT_WIDTH = "viewport.width";
    public static final String VIEWPORT_HEIGHT = "viewport.height";
    public static final String SCALE_FACTOR = "scale.factor";
    public static final String CALC_WIDTH = "calc.width";
    public static final String CALC_HEIGHT = "calc.height";

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
    private String styleTemplate;
    private String wrapStyleTemplate;

    private final Map<String,String> templateProperties = new HashMap<String,String>();

    public StyleableDeviceImpl(String id) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(DeviceManager.class.getName(), Session.get().getLocale());
        this.id = id;
        this.name = resourceBundle.getString(id);
        ResourceBundle properties = ResourceBundle.getBundle(DeviceManager.class.getPackage().getName()+".devices."+id);
        for (String property : properties.keySet()) {
            templateProperties.put(property, properties.getString(property));
        }
        if (properties.containsKey("autoCalc") && "true".equals(properties.getString("autoCalc"))) {
            autoCalcSize();
        }
        if (properties.containsKey("style")) {
            this.styleTemplate = properties.getString("style");
        } else {
            this.styleTemplate = defaultStyleTemplate;
        }
        if (properties.containsKey("wrapStyle")) {
            this.wrapStyleTemplate = properties.getString("wrapStyle");
        } else {
            this.wrapStyleTemplate = defaultWrapStyleTemplate;
        }
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getStyle() {
        return processTemplate(styleTemplate);
    }

    public String getWrapStyle() {
        return processTemplate(wrapStyleTemplate);
    }

    /*
    public StyleableDeviceImpl set(String property, String value) {
        templateProperties.put(property, value);
        return this;
    }

    public StyleableDeviceImpl setStyleTemplate(String styleTemplate) {
        this.styleTemplate = styleTemplate;
        return this;
    }

    public StyleableDeviceImpl setWrapStyleTemplate(String wrapStyleTemplate) {
        this.wrapStyleTemplate = wrapStyleTemplate;
        return this;
    }
    */

    private void autoCalcSize() {
        try {
            int viewPortWidth = Integer.parseInt(templateProperties.get(VIEWPORT_WIDTH));
            int viewPortHeight = Integer.parseInt(templateProperties.get(VIEWPORT_HEIGHT));
            double scaleFactor = Double.parseDouble(templateProperties.get(SCALE_FACTOR));
            if (scaleFactor != 0.0) {
                int cw = (int) Math.floor(viewPortWidth / scaleFactor);
                int ch = (int) Math.floor(viewPortHeight / scaleFactor);
                templateProperties.put(CALC_WIDTH, String.valueOf(cw));
                templateProperties.put(CALC_HEIGHT, String.valueOf(ch));
            }
        } catch (NumberFormatException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String processTemplate(String style) {
        return MapVariableInterpolator.interpolate(style, templateProperties);
    }
}
