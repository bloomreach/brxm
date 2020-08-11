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
package org.hippoecm.hst.demo.addonmodules.api;

public interface MatrixOperator {

    /**
     * Returns the inverse of this matrix.
     * <P>
     * Note that matrix entries are represented as a two-dimensional array.
     * </P>
     * @param matrixData
     * @return
     */
    double [][] inverse(double[][] matrixData);

    /**
     * Returns the multiplication result of two matrices.
     * @param matrixData1
     * @param matrixData2
     * @return
     */
    double[][] multiply(double [][] matrixData1, double [][] matrixData2);

}
