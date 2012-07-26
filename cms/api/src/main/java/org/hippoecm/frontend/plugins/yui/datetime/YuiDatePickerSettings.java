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

import java.io.Serializable;

import org.hippoecm.frontend.plugins.standards.ClassResourceModel;

public class YuiDatePickerSettings implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_DATE_PATTERN = "M/d/yyyy";

    private String datePattern;
    private boolean hideOnSelect = true;
    private boolean fireChangeEvent = true;
    private boolean close = true;

    public YuiDatePickerSettings() {
        ClassResourceModel pickerFormat = new ClassResourceModel("picker-format", YuiDateTimeField.class);
        this.datePattern = pickerFormat.getObject();
    }

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    public boolean isHideOnSelect() {
        return hideOnSelect;
    }

    public void setHideOnSelect(boolean hideOnSelect) {
        this.hideOnSelect = hideOnSelect;
    }

    public boolean isFireChangeEvent() {
        return fireChangeEvent;
    }

    public void setFireChangeEvent(boolean fireChangeEvent) {
        this.fireChangeEvent = fireChangeEvent;
    }

    public boolean isClose() {
        return close;
    }

    public void setClose(boolean close) {
        this.close = close;
    }
}
