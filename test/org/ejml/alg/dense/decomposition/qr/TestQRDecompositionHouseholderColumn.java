/*
 * Copyright (c) 2009-2010, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * EJML is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * EJML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EJML.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ejml.alg.dense.decomposition.qr;

import org.ejml.alg.dense.decomposition.QRDecomposition;
import org.ejml.data.DenseMatrix64F;
import org.ejml.data.SimpleMatrix;
import org.ejml.ops.RandomMatrices;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestQRDecompositionHouseholderColumn extends GenericQrCheck {

    Random rand = new Random(0xff);


    @Override
    protected QRDecomposition createQRDecomposition() {
        return new QRDecompositionHouseholderColumn();
    }

    /**
     * Internall several house holder operations are performed.  This
     * checks to see if the householder operations and the expected result for all the
     * submatrices.
     */
    @Test
    public void householder() {
        int width = 5;

        for( int i = 0; i < width; i++ ) {
            checkSubHouse(i , width);
        }
    }

    private void checkSubHouse(int w , int width) {
        DebugQR qr = new DebugQR(width,width);

        SimpleMatrix A = new SimpleMatrix(width,width);
        RandomMatrices.setRandom(A.getMatrix(),rand);

        qr.householder(w,A.getMatrix());

        SimpleMatrix U = new SimpleMatrix(width,1,qr.getQR()[w]).extractMatrix(w,width-1,0,0);
        U.set(0,0,1); // this is not explicity set and is assumed to be 1
        SimpleMatrix I = SimpleMatrix.identity(width-w);
        SimpleMatrix Q = I.minus(U.mult(U.transpose()).elementMult(qr.getGamma()));


        // check the expected properties of Q
        assertTrue(Q.isIdentical(Q.transpose(),1e-6));
        assertTrue(Q.isIdentical(Q.invert(),1e-6));

        SimpleMatrix result = Q.mult(A.extractMatrix(w,width-1,w,width-1));

        for( int i = 1; i < width-w; i++ ) {
            assertEquals(0,result.get(i,0),1e-5);
        }
    }

    /**
     * Check the results of this function against basic matrix operations
     * which are equivalent.
     */
    @Test
    public void updateA() {
        int width = 5;

        for( int i = 0; i < width; i++ )
            checkSubMatrix(width,i);
    }

    private void checkSubMatrix(int width , int w ) {
        DebugQR qr = new DebugQR(width,width);

        double gamma = 0.2;
        double tau = 0.75;

        SimpleMatrix U = new SimpleMatrix(width,1);
        SimpleMatrix A = new SimpleMatrix(width,width);

        RandomMatrices.setRandom(U.getMatrix(),rand);
        RandomMatrices.setRandom(A.getMatrix(),rand);

        qr.convertToColumnMajor(A.getMatrix());

        // compute the results using standard matrix operations
        SimpleMatrix I = SimpleMatrix.identity(width-w);

        SimpleMatrix u_sub = U.extractMatrix(w,width-1,0,0);
        u_sub.set(0,0,1);// assumed to be 1 in the algorithm
        SimpleMatrix A_sub = A.extractMatrix(w,width-1,w,width-1);
        SimpleMatrix expected = I.minus(u_sub.mult(u_sub.transpose()).elementMult(gamma)).mult(A_sub);

        qr.updateA(w,U.getMatrix().getData(),gamma,tau);

        double[][] found = qr.getQR();

        assertEquals(-tau,found[w][w],1e-8);

        for( int i = w+1; i < width; i++ ) {
            assertEquals(U.get(i,0),found[w][i],1e-8);
        }

        // the right should be the same
        for( int i = w; i < width; i++ ) {
            for( int j = w+1; j < width; j++ ) {
                double a = expected.get(i-w,j-w);
                double b = found[j][i];

                assertEquals(a,b,1e-6);
            }
        }
    }

    private static class DebugQR extends QRDecompositionHouseholderColumn
    {

        public DebugQR( int numRows , int numCols ) {
            setExpectedMaxSize(numRows,numCols);
            this.numCols = numCols;
            this.numRows = numRows;
        }

        public void householder( int j , DenseMatrix64F A ) {
            convertToColumnMajor(A);

            super.householder(j);
        }

        protected void convertToColumnMajor(DenseMatrix64F A) {
            super.convertToColumnMajor(A);
        }

        public void updateA( int w , double u[] , double gamma , double tau ) {
            System.arraycopy(u,0,this.dataQR[w],0,u.length);
            this.gamma = gamma;
            this.tau = tau;

            super.updateA(w);
        }

        public double getGamma() {
            return gamma;
        }
    }
}