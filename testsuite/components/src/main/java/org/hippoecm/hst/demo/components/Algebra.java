/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.components;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.addonmodules.api.MatrixOperator;
import org.hippoecm.hst.demo.addonmodules.api.RandomGenerator;
import org.hippoecm.hst.site.HstServices;

public class Algebra extends BaseHstComponent {

    public static final String RANDOM_NUMBERS_MODULE_NAME = "org.hippoecm.hst.demo.addonmodules.randomnumbers";
    public static final String LINEAR_ALGEBRA_MODULE_NAME = "org.hippoecm.hst.demo.addonmodules.linearalgebra";

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        RandomGenerator randomGenerator = HstServices.getComponentManager().getComponent("randomGenerator", RANDOM_NUMBERS_MODULE_NAME);
        MatrixOperator matrixOperator = HstServices.getComponentManager().getComponent("matrixOperator", LINEAR_ALGEBRA_MODULE_NAME);
        
        double [][] data = new double[2][2];
        
        for (int i = 0; i < 2; i++) {
            double [] randomNums = randomGenerator.generate(2);
            for (int j = 0; j < 2; j++) {
                data[i][j] = randomNums[j];
            }
        }
        
        double [][] inverseData = matrixOperator.inverse(data);
        
        request.setAttribute("matrix", ArrayUtils.toString(data));
        request.setAttribute("inverse", ArrayUtils.toString(inverseData));
    }

}
