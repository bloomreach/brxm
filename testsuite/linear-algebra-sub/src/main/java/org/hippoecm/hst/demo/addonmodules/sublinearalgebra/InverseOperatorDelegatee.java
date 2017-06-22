/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.addonmodules.sublinearalgebra;

import org.hippoecm.hst.demo.addonmodules.api.InverseOperator;
import org.hippoecm.hst.demo.addonmodules.api.MatrixOperator;

public class InverseOperatorDelegatee implements InverseOperator {

    private InverseOperator inverseOperator;

    public void setInverseOperator(final InverseOperator inverseOperator) {
        this.inverseOperator = inverseOperator;
    }

    @Override
    public double[][] inverse(final double[][] matrixData) {
        return inverseOperator.inverse(matrixData);
    }

    @Override
    public MatrixOperator getMatrixOperator() {
        return inverseOperator.getMatrixOperator();
    }

}
