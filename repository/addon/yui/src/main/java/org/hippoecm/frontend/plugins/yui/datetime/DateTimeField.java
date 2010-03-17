/*
 *  Copyright 2008 Hippo.
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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.form.AbstractTextComponent.ITextFormatProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converters.DateConverter;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.util.MappingException;
import org.hippoecm.frontend.util.PluginConfigMapper;
import org.joda.time.DateTime;

/**
 * Semi-fork of YUI DateTimeField from Wicket extensions. Replaces Wicket extensions YUI behaviors with a {@link YuiDatePicker}
 * so it fit's in the Hippo ECM YUI framework.
 * 
 * DatePicker can be configured using a frontend:pluginconfig node with name <code>datepicker</code>.
 * 
 * @see YuiDatePickerSettings
 */
public class DateTimeField extends org.apache.wicket.extensions.yui.calendar.DateTimeField {
    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public DateTimeField(String id, IModel<Date> model, IPluginContext context, IPluginConfig config) {
        super(id, model);
        
        DateTextField dateField = (DateTextField) get("date");
        //remove Wicket extensions YUI picker and add own
        for(IBehavior b : dateField.getBehaviors()) {
            dateField.remove(b);
        }

        YuiDatePickerSettings settings = new YuiDatePickerSettings();
        settings.setDatePattern(getDatePattern());
        
        if(config.containsKey("datepicker")) {
            try {
                PluginConfigMapper.populate(settings, config.getPluginConfig("datepicker"));
            } catch (MappingException e) {
                throw new RuntimeException(e);
            }
        }
        dateField.add(new YuiDatePicker(settings));
    }
    
    private String getDatePattern() {
        String format = null;
        if (this instanceof ITextFormatProvider) {
            format = ((ITextFormatProvider) this).getTextFormat();
            // it is possible that components implement ITextFormatProvider but
            // don't provide a format
        }

        if (format == null) {
            IConverter converter = getConverter(DateTime.class);
            if (!(converter instanceof DateConverter)) {
                converter = getConverter(Date.class);
            }
            format = ((SimpleDateFormat) ((DateConverter) converter).getDateFormat(getLocale())).toPattern();
        }
        return format;
    }
    
}
