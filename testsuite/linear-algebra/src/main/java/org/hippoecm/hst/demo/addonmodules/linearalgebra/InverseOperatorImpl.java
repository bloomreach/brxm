package org.hippoecm.hst.demo.addonmodules.linearalgebra;


import org.hippoecm.hst.demo.addonmodules.api.InverseOperator;
import org.hippoecm.hst.demo.addonmodules.api.MatrixOperator;

public class InverseOperatorImpl implements InverseOperator {

    private MatrixOperatorImpl matrixOperator;

    public void setMatrixOperator(final MatrixOperatorImpl matrixOperator) {
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
