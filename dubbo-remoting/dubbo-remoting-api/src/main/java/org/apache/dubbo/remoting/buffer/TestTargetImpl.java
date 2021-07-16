package org.apache.dubbo.remoting.buffer;

/**
 * @Author: Hanlei
 * @Date: 2021/4/3 8:55 下午
 */
public class TestTargetImpl implements TestTarget {

    @Override
    public void run() {
        // TODO Auto-generated method stub
        System.out.println("run");
        eat();
        ((TestTarget) ProxyFactory.getProxy(new TestTargetImpl())).eat();
    }

    @Override
    public void eat() {
        // TODO Auto-generated method stub
        System.out.println("eat");
    }

    @Override
    public void sleep() {
        // TODO Auto-generated method stub
        System.out.println("sleep");
    }

}
