package com.reservationhub.support;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Shuffles test methods on every run so hidden order dependencies between tests surface early.
 * Registered as a listener in testng.xml. The seed is printed so a failing order can be reproduced
 * with -Dtest.order.seed=<seed>.
 */
public final class RandomOrderInterceptor implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        long seed = Long.getLong("test.order.seed", System.nanoTime());
        System.out.printf("Test order seed: %d (rerun this order with -Dtest.order.seed=%d)%n", seed, seed);
        List<IMethodInstance> shuffled = new ArrayList<>(methods);
        Collections.shuffle(shuffled, new Random(seed));
        return shuffled;
    }
}
