/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.addon.workflow;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.hippoecm.frontend.session.UserSession;

public class FutureDateValidator extends AbstractValidator<Date> {

    public static final String EMPTY_DATE = "date.empty";
    public static final String DATE_IN_THE_PAST = "date.in.past";
    public static final String INPUTDATE_LABEL = "inputdate";
    public static final int SECONDS_ADDED_TO_CORRECT_FOR_UNSET_SECOND_BY_DATETIMEFIELD = 59;
    public static final int YEAR_CORRECTION = 1900;

    private String resourceKey;


    public FutureDateValidator() {
    }


    public boolean validateOnNullValue() {
        return true;
    }

    @Override
    protected void onValidate(final IValidatable<Date> dateIValidatable) {
        Date date = dateIValidatable.getValue();
        if (date == null) {
            resourceKey = EMPTY_DATE;
            error(dateIValidatable);
            return;
        }
        Calendar now = Calendar.getInstance();
        Calendar publicationDate = Calendar.getInstance();
        publicationDate.set(date.getYear() + YEAR_CORRECTION, date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds());
        publicationDate.add(Calendar.SECOND, SECONDS_ADDED_TO_CORRECT_FOR_UNSET_SECOND_BY_DATETIMEFIELD);
        if (publicationDate.before(now)) {
            resourceKey = DATE_IN_THE_PAST;
            error(dateIValidatable);
        }
    }

    @Override
    protected Map<String, Object> variablesMap(IValidatable<Date> validatable) {
        final Map<String, Object> map = super.variablesMap(validatable);
        Date date = validatable.getValue();
        if (date == null) {
            return map;
        }
        UserSession session = (UserSession) org.apache.wicket.Session.get();
        Locale locale = session.getLocale();

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, locale);
        map.put(INPUTDATE_LABEL, df.format(date));

        return map;
    }


    @Override
    protected String resourceKey() {
        return resourceKey;
    }


}
