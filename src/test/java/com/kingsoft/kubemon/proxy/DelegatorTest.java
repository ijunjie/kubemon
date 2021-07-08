package com.kingsoft.kubemon.proxy;

import org.junit.jupiter.api.Test;

class DelegatorTest {

    @Test
    public void testDelegate() {
        Bar bar = new Bar();
        IFoo barDelegator = Delegator.delegate(IFoo.class, bar);
        String r = barDelegator.hello("BarDelegator");
        System.out.println(r);
    }
}