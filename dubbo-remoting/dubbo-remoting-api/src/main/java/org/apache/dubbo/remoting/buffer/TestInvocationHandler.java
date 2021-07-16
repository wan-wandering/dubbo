package org.apache.dubbo.remoting.buffer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @Author: Hanlei
 * @Date: 2021/4/3 10:14 下午
 */
public class TestInvocationHandler implements InvocationHandler {
    private Object targetImpl;

    public <T> TestInvocationHandler(T t) {
       this.targetImpl = t;
    }

    @Override
        // proxy为目标类，menthod为调用的方法，args为参数
        public Object invoke(Object proxy, Method
        method, Object[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

            System.out.println("执行前");
            Object object = method.invoke(targetImpl, args);
            System.out.println("执行后");
            return object;
        }
}
