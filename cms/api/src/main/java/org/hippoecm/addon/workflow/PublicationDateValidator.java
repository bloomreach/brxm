package org.hippoecm.addon.workflow;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

/**
 * $Id$
 */
public class PublicationDateValidator extends AbstractValidator<Date> {

    public static final String EMPTY_PUBLICATION_DATE = "publication.date.empty";
    public static final String PUBLICATION_DATE_IN_THE_PAST = "publication.date.in.past";
    public static final String INPUTDATE_LABEL = "inputdate";

    private String resourceKey;
    private String format;


    private PublicationDateValidator(String format) {
        this.format = format;
    }

    public static PublicationDateValidator format(String format) {
        return new PublicationDateValidator(format);
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
        } else if (date.before(new Date())) {
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
        if (format == null) {
            map.put(INPUTDATE_LABEL, date);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            map.put(INPUTDATE_LABEL, sdf.format(date));
        }
        return map;
    }


    @Override
    protected String resourceKey() {
        return resourceKey;
    }


}
