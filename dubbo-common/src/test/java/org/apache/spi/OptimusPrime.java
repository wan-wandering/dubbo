package org.apache.spi;

import org.apache.dubbo.common.URL;

/**
 * @Author: Hanlei
 * @Date: 2021/3/1 10:29 上午
 */
public class OptimusPrime implements Robot {

    @Override
    public void sayHello() {
        System.out.println("Hello, I am Optimus Prime.");
    }

    @Override
    public void sayHello(URL url) {
        System.out.println("Hello, I am Adaptive Optimus Prime.");
    }
}

