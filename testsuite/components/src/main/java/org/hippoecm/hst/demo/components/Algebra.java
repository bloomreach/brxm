/*
 *  Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.demo.addonmodules.api.InverseOperator;
import org.hippoecm.hst.demo.addonmodules.api.MatrixOperator;
import org.hippoecm.hst.demo.addonmodules.api.RandomGenerator;
import org.hippoecm.hst.demo.addonmodules.linearalgebra.MyMatrixOperator;
import org.hippoecm.hst.site.HstServices;

public class Algebra extends BaseHstComponent {

    public static final String RANDOM_NUMBERS_MODULE_NAME = "org.hippoecm.hst.demo.addonmodules.randomnumbers";
    public static final String LINEAR_ALGEBRA_MODULE_NAME = "org.hippoecm.hst.demo.addonmodules.linearalgebra";
    public static final String SUB_LINEAR_ALGEBRA_MODULE_NAME = "org.hippoecm.hst.demo.addonmodules.sublinearalgebra";

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        RandomGenerator randomGenerator = HstServices.getComponentManager().getComponent("randomGenerator", RANDOM_NUMBERS_MODULE_NAME);
        MatrixOperator matrixOperator = HstServices.getComponentManager().getComponent("matrixOperator", LINEAR_ALGEBRA_MODULE_NAME);

        if (!(matrixOperator instanceof MyMatrixOperator)) {
            throw new AssertionError("matrixOperator is expected to be an instanceof MyMatrixOperator");
        }

        InverseOperator inverseOperator = HstServices.getComponentManager().getComponent("inverseOperator", LINEAR_ALGEBRA_MODULE_NAME);


        if (!(inverseOperator.getMatrixOperator() instanceof MyMatrixOperator)) {
            throw new AssertionError("matrixOperator is expected to be an instanceof MyMatrixOperator");
        }

        InverseOperator inverseOperatorDelegatee = HstServices.getComponentManager().getComponent("inverseOperatorDelegatee", SUB_LINEAR_ALGEBRA_MODULE_NAME);
        if (!(inverseOperatorDelegatee.getMatrixOperator() instanceof MyMatrixOperator)) {
            throw new AssertionError("matrixOperator is expected to be an instanceof MyMatrixOperator");
        }

        double [][] matrixData = new double[2][2];

        for (int i = 0; i < 2; i++) {
            double [] randomNums = randomGenerator.generate(2);
            for (int j = 0; j < 2; j++) {
                matrixData[i][j] = randomNums[j];
            }
        }

        double [][] inverseMatrixData = matrixOperator.inverse(matrixData);

        request.setAttribute("matrix", ArrayUtils.toString(matrixData));
        request.setAttribute("inverse", ArrayUtils.toString(inverseMatrixData));

        double [][] multiplied = matrixOperator.multiply(matrixData, inverseMatrixData);
        request.setAttribute("multiplied", ArrayUtils.toString(multiplied));
    }

}
