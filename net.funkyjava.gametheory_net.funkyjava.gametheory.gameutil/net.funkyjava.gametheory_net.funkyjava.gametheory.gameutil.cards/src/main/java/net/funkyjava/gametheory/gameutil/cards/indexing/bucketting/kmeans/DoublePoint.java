/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.funkyjava.gametheory.gameutil.cards.indexing.bucketting.kmeans;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A simple implementation of {@link Clusterable} for points with double coordinates.
 * @version $Id: DoublePoint.java 1461862 2013-03-27 21:48:10Z tn $
 * @since 3.2
 */
public class DoublePoint implements Clusterable, Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 3946024775784901369L;

    /** Point coordinates. */
    private double[] point;
    
    /** Count */
    private int n = 1;
    
    /** Index */
    private int index;

    /**
     * Build an instance wrapping an double array.
     * <p>
     * The wrapped array is referenced, it is <em>not</em> copied.
     *
     * @param point the n-dimensional point in double space
     */
    public DoublePoint(final double... point) {
        this.point = point;
    }
     
    /**
     * Build an instance wrapping an double array.
     * <p>
     * The wrapped array is referenced, it is <em>not</em> copied.
     *
     * @param count the absolute frequency of this point
     * @param point the n-dimensional point in double space
     */    
    public DoublePoint(final int count, final double... point) {
    	this.point = point;
    	this.n = count;
    }
    
    public DoublePoint(final int index, final int count, final double... point) {
    	this.point = point;
    	this.n = count;
    	this.index = index;
    }

    /**
     * Build an instance wrapping an integer array.
     * <p>
     * The wrapped array is copied to an internal double array.
     *
     * @param point the n-dimensional point in integer space
     */
    public DoublePoint(final int... point) {
        this.point = new double[point.length];
        for ( int i = 0; i < point.length; i++) {
            this.point[i] = point[i];
        }
    }
    
    public void setPoint(final double... point) {
    	this.point = point;
    }

    /** {@inheritDoc} */
    public double[] getPoint() {
        return point;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DoublePoint)) {
            return false;
        }
        return Arrays.equals(point, ((DoublePoint) other).point);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(point);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Arrays.toString(point);
    }

    /** {@inheritDoc} */
	@Override
	public int getCount() {
		return n;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void add(int n) {
		this.n += n;
	}

}
