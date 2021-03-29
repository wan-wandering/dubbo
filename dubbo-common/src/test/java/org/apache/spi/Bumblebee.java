package org.apache.spi;

import org.apache.dubbo.common.URL;

/**
 * @Author: Hanlei
 * @Date: 2021/3/1 10:31 上午
 */
public class Bumblebee implements Robot {

    @Override
    public void sayHello() {
        System.out.println("Hello, I am Bumblebee.");
    }

    @Override
    public void sayHello(URL url) {
        System.out.println("Hello, I am Adaptive Optimus Prime.");
    }
}
