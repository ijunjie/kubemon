package com.kingsoft.kubemon.thread;

public class TestStack {
    private int counter = 0;

    private void recur() {
        counter++;
        recur();//递归
    }

    public void getStackDepth() {
        try {
            recur();
        } catch (Throwable t) {
            System.out.println("栈最大深度：" + counter);
            t.printStackTrace();
        }

    }

    public static void main(String[] args) {
        TestStack stack = new TestStack();
        stack.getStackDepth();

    }

}