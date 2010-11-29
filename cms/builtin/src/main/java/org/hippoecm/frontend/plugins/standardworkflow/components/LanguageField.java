package org.hippoecm.frontend.plugins.standardworkflow.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;

public class LanguageField extends WebMarkupContainer {
    private static final long serialVersionUID = 1L;

    private final DropDownChoice<String> languageChoice;

    public LanguageField(String id, IModel<String> languageModel, final ILocaleProvider provider) {
        super(id);

        List<String> languages = Collections.emptyList();
        if (provider != null) {
            List<? extends HippoLocale> locales = provider.getLocales();
            ArrayList<HippoLocale> ordered = new ArrayList<HippoLocale>(locales);
            Collections.sort(ordered, new Comparator<HippoLocale>() {

                @Override
                public int compare(HippoLocale o1, HippoLocale o2) {
                    return o1.getDisplayName(getLocale()).compareTo(o2.getDisplayName(getLocale()));
                }

            });
            languages = new ArrayList<String>(ordered.size());
            for (HippoLocale locale : ordered) {
                languages.add(locale.getName());
            }
        }
        add(languageChoice = new DropDownChoice<String>("select", languageModel, languages,
                new IChoiceRenderer<String>() {
                    private static final long serialVersionUID = 1L;

                    @SuppressWarnings("null")
                    public Object getDisplayValue(String object) {
                        return provider.getLocale(object).getDisplayName(getLocale());
                    }

                    public String getIdValue(String object, int index) {
                        return object;
                    }

                }));
        languageChoice.setNullValid(false);
        languageChoice.setRequired(true);
        setOutputMarkupPlaceholderTag(true);
    }

}
