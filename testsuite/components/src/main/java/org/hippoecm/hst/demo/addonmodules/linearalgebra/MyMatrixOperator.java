package org.hippoecm.hst.demo.addonmodules.linearalgebra;


import org.hippoecm.hst.demo.addonmodules.api.MatrixOperator;

public class MyMatrixOperator implements MatrixOperator{

    private MatrixOperator delegatee;

    public MyMatrixOperator() {
        this.delegatee = new MatrixOperatorImpl();
    }

    @Override
    public double[][] inverse(final double[][] matrixData) {
        return delegatee.inverse(matrixData);
    }

    @Override
    public double[][] multiply(final double[][] matrixData1, final double[][] matrixData2) {
        return delegatee.multiply(matrixData1, matrixData2);
    }
}
