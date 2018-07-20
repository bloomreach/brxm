/*
 * Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.standardworkflow.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;

public class LanguageField extends WebMarkupContainer {
    private static final long serialVersionUID = 1L;

    public LanguageField(String id, IModel<String> languageModel, final ILocaleProvider provider) {
        super(id);

        List<String> languages = Collections.emptyList();
        if (provider != null) {
            List<? extends HippoLocale> locales = provider.getLocales();
            ArrayList<HippoLocale> ordered = new ArrayList<>(locales);
            Collections.sort(ordered, new Comparator<HippoLocale>() {

                @Override
                public int compare(HippoLocale o1, HippoLocale o2) {
                    return o1.getDisplayName(getLocale()).compareTo(o2.getDisplayName(getLocale()));
                }

            });
            languages = new ArrayList<>(ordered.size());
            for (HippoLocale locale : ordered) {
                languages.add(locale.getName());
            }
        }

        final DropDownChoice<String> languageChoice;
        add(languageChoice = new DropDownChoice<>("select", languageModel, languages,
                new IChoiceRenderer<String>() {
                    private static final long serialVersionUID = 1L;

                    public Object getDisplayValue(String object) {
                        if (provider == null) {
                            return object;
                        }
                        return provider.getLocale(object).getDisplayName(getLocale());
                    }

                    public String getIdValue(String object, int index) {
                        return object;
                    }

                    @Override
                    public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
                        List<? extends String> choices = choicesModel.getObject();
                        return choices.stream().filter(choice -> choice.equals(id)).findFirst().orElse(null);
                    }
                }));
        languageChoice.setNullValid(false);
        languageChoice.setRequired(true);
        languageChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                LanguageField.this.onSelectionChanged();
            }
        });
        setOutputMarkupPlaceholderTag(true);
    }

    protected void onSelectionChanged() {
    }

}
