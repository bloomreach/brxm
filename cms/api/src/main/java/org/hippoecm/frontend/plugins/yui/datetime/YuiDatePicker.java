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

package org.hippoecm.frontend.plugins.yui.datetime;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.markup.html.form.AbstractTextComponent.ITextFormatProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.joda.time.DateTime;

public class YuiDatePicker extends AbstractYuiBehavior {

    private static final ResourceReference SKIN = new PackageResourceReference(YuiDatePicker.class, "resources/skin.css");

    private Component component;
    private DynamicTextTemplate template;

    public YuiDatePicker(YuiDatePickerSettings settings) {
        PackageTextTemplate init = new PackageTextTemplate(YuiDatePicker.class, "resources/init.js");
        this.template = new DynamicTextTemplate(init, settings) {
            @Override
            protected Map<String, Object> getVariables() {
                Map<String, Object> vars = super.getVariables();
                vars.put("id", component.getMarkupId());
                return vars;
            }
        };
    }

    @Override
    public void addHeaderContribution(IYuiContext helper) {
        helper.addCssReference(SKIN);
        helper.addModule(HippoNamespace.NS, "datetime");
        helper.addOnDomLoad(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return "YAHOO.hippo.DateTime.render('" + component.getMarkupId() + "', " + template.getConfigurationAsJSON() + ");";
            }
        });
    }

    @Override
    public void afterRender(Component component) {
        super.afterRender(component);

        // Append the span and img icon right after the rendering of the
        // component. Not as pretty as working with a panel etc, but works
        // for behaviors and is more efficient
        Response response = component.getResponse();
        response.write("\n<div class=\"yui-skin-sam\">&nbsp;<span style=\"");
        if (renderOnLoad()) {
            response.write("display:block;");
        } else {
            response.write("display:none;");
            response.write("position:absolute;");
        }
        response.write("z-index: 99999;\" id=\"");
        response.write(getEscapedComponentMarkupId());
        response.write("Dp\"></span><a style=\"");
        response.write(getIconStyle());
        response.write("\" id=\"");
        response.write(getIconId());
        response.write("\">");
        response.write(getIcon().getSpriteReference(IconSize.M));
        response.write("</a>");

        if (renderOnLoad()) {
            response.write("<br style=\"clear:left;\"/>");
        }

        response.write("</div>");
    }

    /**
     * Indicates whether the calendar should be rendered after it has been loaded.
     *
     * @return <code>true</code> if the calendar should be rendered after it has been loaded.<br/> <code>false</code>
     *         (default) if it's initially hidden.
     */
    protected boolean renderOnLoad() {
        return false;
    }

    /**
     * Gets the escaped DOM id that the calendar widget will get attached to. All non word characters (\W) will be
     * removed from the string.
     *
     * @return The DOM id of the calendar widget - same as the component's markup id + 'Dp'}
     */
    protected final String getEscapedComponentMarkupId() {
        return component.getMarkupId().replaceAll("\\W", "");
    }

    /**
     * Gets the id of the icon that triggers the popup.
     *
     * @return The id of the icon
     */
    protected final String getIconId() {
        return getEscapedComponentMarkupId() + "Icon";
    }

    /**
     * Gets the style of the icon that triggers the popup.
     *
     * @return The style of the icon, e.g. 'cursor: point' etc.
     */
    protected String getIconStyle() {
        return "cursor: pointer; border: none;";
    }

    /**
     * Gets the {@link org.hippoecm.frontend.skin.Icon} for the popup button. Users can override to provide their own icon.

     * @return the icon to use for the popup button
     */
    protected Icon getIcon() {
        return Icon.CALENDAR_MONTH;
    }

    /**
     * Gets the locale that should be used to configure this widget.
     *
     * @return By default the locale of the bound component.
     */
    protected Locale getLocale() {
        return component.getLocale();
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        this.component = component;
        checkComponentProvidesDateFormat(component);
        component.setOutputMarkupId(true);
    }

    /**
     * Check that this behavior can get a date format out of the component it is coupled to. It checks whether {@link
     * #getDatePattern()} produces a non-null value. If that method returns null, and exception will be thrown
     *
     * @param component the component this behavior is being coupled to
     * @throws UnableToDetermineFormatException
     *          if this date picker is unable to determine a format.
     */
    @SuppressWarnings("unused")
    private void checkComponentProvidesDateFormat(Component component) {
        if (getDatePattern() == null) {
            throw new UnableToDetermineFormatException();
        }
    }

    /**
     * Gets the date pattern to use for putting selected values in the coupled component.
     *
     * @return The date pattern
     */
    protected String getDatePattern() {
        String format = null;
        if (component instanceof ITextFormatProvider) {
            format = ((ITextFormatProvider) component).getTextFormat();
            // it is possible that components implement ITextFormatProvider but
            // don't provide a format
        }

        if (format == null) {
            IConverter converter = component.getConverter(DateTime.class);
            if (!(converter instanceof DateConverter)) {
                converter = component.getConverter(Date.class);
            }
            format = ((DateConverter) converter).getDatePattern(component.getLocale());
        }

        return format;
    }

    /**
     * Exception thrown when the bound component does not produce a format this date picker can work with.
     */
    private static final class UnableToDetermineFormatException extends WicketRuntimeException {
        private static final long serialVersionUID = 1L;

        public UnableToDetermineFormatException() {
            super("This behavior can only be added to components that either implement " +
                    ITextFormatProvider.class.getName() +
                    " AND produce a non-null format, or that use" +
                    " converters that this datepicker can use to determine" +
                    " the pattern being used. Alternatively, you can extend " +
                    " the date picker and override getDatePattern to provide your own");
        }
    }


}
