package com.kingsoft.kubemon.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 将一个鸭子类型对象纳入到已有的继承关系中
 * 例如, Foo 实现 IFoo, 而 Bar 是鸭子类型
 * Delegator.delegate(IFoo.class, new Bar());
 * 返回的代理对象中被委托的对象实际是 Bar 对象
 */
public final class Delegator {
    private Delegator() {
        throw new RuntimeException("Instantiation not allowed.");
    }

    @SuppressWarnings("all")
    public static <I, T> I delegate(Class<I> interfaceClazz, final T t) {
        return (I) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{interfaceClazz},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return t.getClass().getMethod(method.getName(), mapToClass(args))
                                .invoke(t, args);
                    }
                }
        );
    }


    private static Class<?>[] mapToClass(Object[] args) {
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ArrayList) {
                parameterTypes[i] = List.class;
                continue;
            }
            if (args[i] instanceof LinkedList) {
                parameterTypes[i] = List.class;
                continue;
            }
            if (args[i] instanceof HashMap) {
                parameterTypes[i] = Map.class;
                continue;
            }
            if (args[i] instanceof Long) {
                parameterTypes[i] = long.class;
                continue;
            }
            if (args[i] instanceof Double) {
                parameterTypes[i] = double.class;
                continue;
            }
            if (args[i] instanceof TimeUnit) {
                parameterTypes[i] = TimeUnit.class;
                continue;
            }
            parameterTypes[i] = args[i].getClass();
        }
        return parameterTypes;
    }
}