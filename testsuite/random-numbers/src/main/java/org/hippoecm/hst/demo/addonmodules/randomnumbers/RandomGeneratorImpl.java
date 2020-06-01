/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.addonmodules.randomnumbers;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.UniformRandomGenerator;
import org.hippoecm.hst.demo.addonmodules.api.RandomGenerator;

public class RandomGeneratorImpl implements RandomGenerator {

    private UniformRandomGenerator generator;

    public RandomGeneratorImpl() {
        JDKRandomGenerator rg = new JDKRandomGenerator();
        rg.setSeed(System.currentTimeMillis());
        generator = new UniformRandomGenerator(rg);
    }

    public double[] generate(int n) {
        double[] nums = new double[n];
        
        for (int i = 0; i < n; i++) {
            nums[i] = generator.nextNormalizedDouble();
        }
        
        return nums;
    }

}
