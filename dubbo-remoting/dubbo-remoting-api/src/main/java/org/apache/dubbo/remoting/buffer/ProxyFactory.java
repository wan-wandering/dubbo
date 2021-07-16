package org.apache.dubbo.remoting.buffer;

/**
 * @Author: Hanlei
 * @Date: 2021/4/3 8:56 下午
 */
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyFactory {

    public static <T> Object getProxy(T t){
        Object object = Proxy.newProxyInstance(
                t.getClass().getClassLoader(),
                // 得到参数t的接口对象
                t.getClass().getInterfaces(),
                new TestInvocationHandler(t));

        return object;
    }

    public static void main(String[] args) {

        TestTarget target = (TestTarget) ProxyFactory.getProxy(new TestTargetImpl());

        target.run();

    }
}
