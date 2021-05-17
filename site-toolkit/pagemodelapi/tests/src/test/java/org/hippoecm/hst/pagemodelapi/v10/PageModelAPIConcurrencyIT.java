/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagemodelapi.v10;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jcr.Session;

import org.assertj.core.api.Assertions;
import org.hippoecm.hst.pagemodelapi.common.AbstractPageModelApiITCases;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * This test compares PMA results under concurrency: mostly it validates whether the dynamic bean creation is done
 * correctly
 */
public class PageModelAPIConcurrencyIT extends AbstractPageModelApiITCases {

    @Test
    public void concurrent_PMA_test_dynamic_compound() throws Exception {

        final ExecutorService executorService = Executors.newFixedThreadPool(5);

        final List<Callable<String>> tasks = new ArrayList<>();

        final Session session = createSession("admin", "admin");
        final Random random = new Random();

        for (int i = 0 ; i < 50; i++) {
            tasks.add(() -> {
                DeterministicJsonPointerFactory.reset();

                if (random.nextInt(100) < 10) {
                    synchronized (session) {
                        // touch namespace to trigger reload of dynamic beans
                        session.getNode("/hippo:namespaces/pagemodelapitest/dynamiccontent/hipposysedit:prototypes/hipposysedit:prototype")
                                .setProperty("hippostdpubwf:createdBy", "admin");
                        session.save();
                    }
                }

                final String s = getActualJson("/spa/resourceapi/genericdetail/dynamiccontent");
                return s;
            });
        }

        final List<Future<String>> futures = executorService.invokeAll(tasks);

        executorService.awaitTermination(10, TimeUnit.SECONDS);

        DeterministicJsonPointerFactory.reset();
        final String actualJson = getActualJson("/spa/resourceapi/genericdetail/dynamiccontent");

        futures.stream().forEach(future -> {
            try {
                Assertions.assertThat(future.get())
                        .isEqualTo(actualJson);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });

    }

}