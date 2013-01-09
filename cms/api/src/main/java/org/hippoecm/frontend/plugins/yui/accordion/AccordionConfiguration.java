/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.accordion;

import java.io.Serializable;

public class AccordionConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean throttleUpdate = true;
    private int timeoutLength = 200;
    private String ancestorClassname = "yui-layout-bd";
    private String unitClassname = "hippo-accordion-unit";
    private int unitHeaderHeight = 25;
    private boolean calculateTotalHeight = false;
    private boolean registerResizeListener = true;
    private boolean registerRenderListener = true;

    public void setThrottleUpdate(boolean throttleUpdate) {
        this.throttleUpdate = throttleUpdate;
    }

    public boolean isThrottleUpdate() {
        return throttleUpdate;
    }

    public void setTimeoutLength(int timeoutLength) {
        this.timeoutLength = timeoutLength;
    }

    public int getTimeoutLength() {
        return timeoutLength;
    }

    public void setAncestorClassname(String ancestorClassname) {
        this.ancestorClassname = ancestorClassname;
    }

    public String getAncestorClassname() {
        return ancestorClassname;
    }

    public void setUnitClassname(String unitClassname) {
        this.unitClassname = unitClassname;
    }

    public String getUnitClassname() {
        return unitClassname;
    }

    public void setUnitHeaderHeight(int unitHeaderHeight) {
        this.unitHeaderHeight = unitHeaderHeight;
    }

    public int getUnitHeaderHeight() {
        return unitHeaderHeight;
    }

    public void setCalculateTotalHeight(boolean calculateTotalHeight) {
        this.calculateTotalHeight = calculateTotalHeight;
    }

    public boolean isCalculateTotalHeight() {
        return calculateTotalHeight;
    }

    public void setRegisterResizeListener(boolean registerResizeListener) {
        this.registerResizeListener = registerResizeListener;
    }

    public boolean isRegisterResizeListener() {
        return registerResizeListener;
    }

    public void setRegisterRenderListener(boolean registerRenderListener) {
        this.registerRenderListener = registerRenderListener;
    }

    public boolean isRegisterRenderListener() {
        return registerRenderListener;
    }

}
