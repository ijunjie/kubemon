package com.kingsoft.kubemon.proxy;

/*
 * Bar 跟 IFoo 没有 implements 关系
 * 但实际上实现了 IFoo 的方法, 鸭子类型
 */
public class Bar {
    public String hello(String name) {
        return "[Bar]Hello " + name;
    }
}
