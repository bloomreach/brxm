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
package org.hippoecm.hst.demo.addonmodules.linearalgebra;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.hippoecm.hst.demo.addonmodules.api.MatrixOperator;

public class MatrixOperatorImpl implements MatrixOperator {

    public double[][] inverse(double[][] matrixData) {
        RealMatrix m = new Array2DRowRealMatrix(matrixData);
        return new LUDecompositionImpl(m).getSolver().getInverse().getData();
    }

    public double[][] multiply(double [][] matrixData1, double [][] matrixData2) {
        RealMatrix m1 = new Array2DRowRealMatrix(matrixData1);
        RealMatrix m2 = new Array2DRowRealMatrix(matrixData2);
        RealMatrix result = m1.multiply(m2);
        return result.getData();
    }
}
