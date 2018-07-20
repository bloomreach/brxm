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
package org.hippoecm.frontend.i18n.types;

import java.text.Collator;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;

/**
 * Type choice renderer that sorts the types alphabetically by the translated value of the type using the
 * default {@link Collator} for the current CMS locale. This renderer is also the (sorted) list of types to render.
 * To use it in (for example) a DropDownChoice, use the following code:
 * <pre>
 *   Collection<String> myTypeList = ...
 *   SortedTypeChoiceRenderer renderer = new SortedTypeChoiceRenderer(myComponent, myTypeList);
 *   DropDownChoice dropDown = new DropDownChoice("wicketId", typeModel, renderer, renderer);
 * </pre>
 */
public class SortedTypeChoiceRenderer extends AbstractList<String> implements IChoiceRenderer<String> {

    private static final long serialVersionUID = 1L;

    private final List<Choice> choices;
    private final Map<String, String> typeToTranslationMap;

    public SortedTypeChoiceRenderer(Component component, Collection<String> types) {
        this(types, component.getSession().getLocale(), component);
    }

    public SortedTypeChoiceRenderer(Collection<String> types, Locale locale) {
        this(types, locale, null);
    }

    /**
     * Creates a new choice renderer the given types in the given locale. The resource bundle of the given component
     * is used as a backup to lookup the translated type names in case the repository does not contain a translation
     * for it.
     *
     * @param types the JCR types to translate
     * @param locale the locale to translate the types into
     * @param component the component the component whose resource bundle is used as the backup for looking up
     *                  translated type names. When <code>null</code>, no backup will be used.
     */
    public SortedTypeChoiceRenderer(Collection<String> types, Locale locale, Component component) {
        choices = new ArrayList<Choice>(types.size());
        typeToTranslationMap = new HashMap<String, String>();

        for (String type: types) {
            String translation = translateType(component, type);
            typeToTranslationMap.put(type, translation);

            final Choice choice = new Choice(type, translation);
            choices.add(choice);
        }

        // sort the list of choices alphabetically on the display value using the Collator for the given locale
        final Comparator<Choice> choiceComparator = new ChoiceComparator(Collator.getInstance(locale));
        Collections.sort(choices, choiceComparator);
    }

    private static String translateType(Component component, String type) {
        JcrNodeTypeModel nodeTypeModel = new JcrNodeTypeModel(type);
        if (nodeTypeModel.getObject() != null) {
            return new TypeTranslator(nodeTypeModel).getTypeName().getObject();
        } else if (component != null) {
            return new StringResourceModel(type, component)
                    .setDefaultValue(type)
                    .getString();
        } else {
            return type;
        }
    }

    @Override
    public String get(final int index) {
        return choices.get(index).getId();
    }

    @Override
    public int size() {
        return choices.size();
    }

    public String getIdValue(String type, int index) {
        return type;
    }

    @Override
    public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
        final List<? extends String> choices = choicesModel.getObject();
        return choices.stream().filter(choice -> choice.equals(id)).findFirst().orElse(null);
    }

    public Object getDisplayValue(String type) {
        return typeToTranslationMap.get(type);
    }

    private static class Choice implements IClusterable {

        private static final long serialVersionUID = 1L;

        private String id;
        private String displayValue;

        Choice(String id, String displayValue) {
            this.id = id;
            this.displayValue = displayValue;
        }

        public String getId() {
            return id;
        }

        public String getDisplayValue() {
            return displayValue;
        }

    }

    private static class ChoiceComparator implements Comparator<Choice> {

        private final Collator collator;

        ChoiceComparator(Collator collator) {
            this.collator = collator;
        }
        
        @Override
        public int compare(final Choice c1, final Choice c2) {
            return collator.compare(c1.getDisplayValue(), c2.getDisplayValue());
        }
    }

}
