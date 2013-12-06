package org.onehippo.cms7.essentials.installer;

import org.apache.wicket.extensions.markup.html.form.select.SelectOption;

/**
 * @version "$Id$"
 */
public interface Option<T> {

   public String getOptionGroup();

   public SelectOption<T> getSelectOption();

}
