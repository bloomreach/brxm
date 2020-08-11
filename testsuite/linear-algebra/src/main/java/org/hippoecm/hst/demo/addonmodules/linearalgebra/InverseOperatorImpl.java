/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.addonmodules.linearalgebra;


import org.hippoecm.hst.demo.addonmodules.api.InverseOperator;
import org.hippoecm.hst.demo.addonmodules.api.MatrixOperator;

public class InverseOperatorImpl implements InverseOperator {

    private MatrixOperator matrixOperator;

    public void setMatrixOperator(final MatrixOperator matrixOperator) {
        this.matrixOperator = matrixOperator;
    }

    public double[][] inverse(double[][] matrixData) {
        return matrixOperator.inverse(matrixData);
    }

    @Override
    public MatrixOperator getMatrixOperator() {
        return matrixOperator;
    }
}
