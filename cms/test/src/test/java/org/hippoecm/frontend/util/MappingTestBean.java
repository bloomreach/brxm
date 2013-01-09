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
package org.hippoecm.frontend.util;

public class MappingTestBean {

    private int testInt;
    private boolean testBool;
    private String testString;
    private double testDouble;

    public void setTestInt(int testInt) {
        this.testInt = testInt;
    }

    public int getTestInt() {
        return testInt;
    }

    public void setTestBool(boolean testBool) {
        this.testBool = testBool;
    }

    public boolean isTestBool() {
        return testBool;
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }

    public String getTestString() {
        return testString;
    }

    public void setTestDouble(double testDouble) {
        this.testDouble = testDouble;
    }

    public double getTestDouble() {
        return testDouble;
    }
}
