package com.kingsoft.kubemon.proxy;

public class FooImpl implements IFoo {
    @Override
    public String hello(String name) {
        return "[FooImpl]Hello " + name;
    }
}
