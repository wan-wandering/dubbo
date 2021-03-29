package org.apache.spi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

/**
 * @Author: Hanlei
 * @Date: 2021/3/1 10:30 上午
 */
@SPI
public interface Robot {

    void sayHello();
    @Adaptive
    void sayHello(URL url);

}
