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

import java.util.Date;
import java.util.TimeZone;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.datetime.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

public class AjaxDateTimeField extends DateTimeField {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: AjaxDateTimeField.java 19048 2009-07-28 16:08:33Z abogaart $";

    private static final long serialVersionUID = 1L;

    public AjaxDateTimeField(String id, IModel model, boolean todayLinkVisible, IPluginContext context, IPluginConfig config) {
        super(id, model, context, config);

        get("date").add(new ChangeBehaviour());
        get("hours").add(new ChangeBehaviour());
        get("minutes").add(new ChangeBehaviour());
        get("amOrPmChoice").add(new ChangeBehaviour());

        setOutputMarkupId(true);

        add(new AjaxLink("today") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateDateTime(new Date(), null, null, target);
            }
        }.setVisible(todayLinkVisible));
    }

    // callback that the ChangeBehaviour calls when one of the composing fields updates
    public void onUpdate(AjaxRequestTarget target) {
        Object dateFieldInput = getField("date").getModelObject();
        Integer hours = (Integer) getField("hours").getModelObject();
        Integer minutes = (Integer) getField("minutes").getModelObject();

        updateDateTime(dateFieldInput, hours, minutes, target);
    }

    private void updateDateTime(Object datetime, Integer hours, Integer minutes, AjaxRequestTarget target) {
        MutableDateTime date = new MutableDateTime(datetime);
        try {
            TimeZone zone = getClientTimeZone();
            if (zone != null) {
                date.setZone(DateTimeZone.forTimeZone(zone));
            }

            if (hours != null) {
                date.set(DateTimeFieldType.hourOfDay(), hours.intValue() % 24);
                date.setMinuteOfHour((minutes != null) ? minutes.intValue() : 0);
            }

            // the date will be in the server's timezone
            setModelObject(date.toDate());

            if (target != null) {
                target.addComponent(this);
            }
        } catch (RuntimeException e) {
            error(e.getMessage());
            invalid();
        }
    }

    private FormComponent getField(String id) {
        return (FormComponent) get(id);
    }

    @Override
    protected boolean use12HourFormat() {
        return false;
    }

    class ChangeBehaviour extends AjaxFormComponentUpdatingBehavior {
        private static final long serialVersionUID = 1L;

        ChangeBehaviour() {
            super("onChange");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            AjaxDateTimeField.this.onUpdate(target);
        }
    }
}
