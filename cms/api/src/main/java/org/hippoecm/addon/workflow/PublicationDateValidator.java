package org.hippoecm.addon.workflow;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.hippoecm.frontend.session.UserSession;

/**
 * $Id$
 */
public class PublicationDateValidator extends AbstractValidator<Date> {

    public static final String EMPTY_PUBLICATION_DATE = "publication.date.empty";
    public static final String PUBLICATION_DATE_IN_THE_PAST = "publication.date.in.past";
    public static final String INPUTDATE_LABEL = "inputdate";
    public static final int SECONDS_ADDED_TO_CORRECT_FOR_UNSET_SECOND_BY_DATETIMEFIELD = 59;
    public static final int YEAR_CORRECTION = 1900;

    private String resourceKey;


    public PublicationDateValidator() {
    }


    public boolean validateOnNullValue() {
        return true;
    }

    @Override
    protected void onValidate(final IValidatable<Date> dateIValidatable) {
        Date date = dateIValidatable.getValue();
        if (date == null) {
            resourceKey = EMPTY_PUBLICATION_DATE;
            error(dateIValidatable);
            return;
        }
        Calendar now = Calendar.getInstance();
        Calendar publicationDate = Calendar.getInstance();
        publicationDate.set(date.getYear() + YEAR_CORRECTION, date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds());
        publicationDate.add(Calendar.SECOND, SECONDS_ADDED_TO_CORRECT_FOR_UNSET_SECOND_BY_DATETIMEFIELD);
        if (publicationDate.before(now)) {
            resourceKey = PUBLICATION_DATE_IN_THE_PAST;
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
