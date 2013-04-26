package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

import org.apache.cxf.common.util.StringUtils;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class StyleableAutoCalculatingDeviceModel extends StyleableTemplateDeviceModel {

    private static Logger log = LoggerFactory.getLogger(StyleableAutoCalculatingDeviceModel.class);
    protected static final String BACKGROUND_WIDTH = "background.width";
    protected static final String BACKGROUND_HEIGHT = "background.height";
    protected static final String VIEWPORT_X = "viewport.x";
    protected static final String VIEWPORT_Y = "viewport.y";
    protected static final String VIEWPORT_WIDTH = "viewport.width";
    protected static final String VIEWPORT_HEIGHT = "viewport.height";
    protected static final String SCALE_FACTOR = "scale.factor";
    protected static final String CALC_WIDTH = "calc.width";
    protected static final String CALC_HEIGHT = "calc.height";

    private final int viewPortWidth;
    private final int viewPortHeight;
    private final double scaleFactor;

    public final static String styleTemplate = "" +
            "width: ${calc.width}px!important;\n" +
            "height: ${calc.height}px!important;\n" +
            "transform: scale(${scale.factor},${scale.factor}); \n" +
            "-ms-transform: scale(${scale.factor},${scale.factor}); \n" +
            "-webkit-transform: scale(${scale.factor},${scale.factor}); \n" +
            "-o-transform: scale(${scale.factor},${scale.factor}); \n" +
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

    public final static String wrapTemplate = "" +
            "background: url('${binary.background}') 0 0 no-repeat; \n" +
            "width: ${background.width}px ;\n" +
            "height:${background.height}px ;\n" +
            "background-position: 0px 0px;\n" +
            "background-repeat: no-repeat no-repeat;\n" +
            "position: relative;\n" +
            "border: none;";


    public StyleableAutoCalculatingDeviceModel(final IPluginConfig config) {
        super(config);

        if (!containsKeys(config, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, VIEWPORT_X, VIEWPORT_Y, VIEWPORT_WIDTH, VIEWPORT_HEIGHT, SCALE_FACTOR)) {
            throw new IllegalArgumentException("Autocalculating plugin is missing some parameters please check you configuration");
        }

        viewPortWidth = config.getInt(VIEWPORT_WIDTH);
        viewPortHeight = config.getInt(VIEWPORT_HEIGHT);
        scaleFactor = config.getDouble(SCALE_FACTOR);
    }

    @Override
    public String getStyle() {
        final String superStyle = super.getStyle();

        int cw = (int) Math.floor(viewPortWidth / scaleFactor);
        int ch = (int) Math.floor(viewPortHeight / scaleFactor);

        add(CALC_WIDTH, cw);
        add(CALC_HEIGHT, ch);

        if (StringUtils.isEmpty(superStyle)) {
            return process(styleTemplate);
        }
        return process(superStyle);
    }

    @Override
    public String getWrapStyle() {
        final String superWrapStyle = super.getWrapStyle();
        if(StringUtils.isEmpty(superWrapStyle)){
            return process(wrapTemplate);
        }
        return process(superWrapStyle);
    }

    private boolean containsKeys(final IPluginConfig config, String... keys) {
        for (String key : keys) {
            if (!config.containsKey(key)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("StyleableAutoCalculatingDeviceModel");
        sb.append("{viewPortWidth=").append(viewPortWidth);
        sb.append(", viewPortHeight=").append(viewPortHeight);
        sb.append(", scaleFactor=").append(scaleFactor);
        sb.append(", style='").append(getStyle()).append('\'');
        sb.append(", wrapStyle='").append(getWrapStyle()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
