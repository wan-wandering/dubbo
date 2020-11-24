/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.DemoService;

import java.util.concurrent.CountDownLatch;
//此方法是demo的启动方式，真正启动时，是读取配置文件的所有服务
public class Application {
    /**启动此方法需要  删除dubbo-config-api | dubbo-rpc-dubbo
    和dubbo-rpc-rest依赖 pom.xml文件中的测试作用域。<scope>test<scope/>
     注册中心也需要启动 默认端口号2181*/
    public static void main(String[] args) throws Exception {
        //选择启动方式，旧方式还是最新的启动方式
        if (isClassic(args)) {
            startWithExport();
        } else {
            startWithBootstrap();
        }
    }

    private static boolean isClassic(String[] args) {
        return args.length > 0 && "classic".equalsIgnoreCase(args[0]);
    }
    //新的启动方法， 主要研究这里启动方式的源码
    private static void startWithBootstrap() {
        //封装服务提供者， ServiceConfig
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        //dubbo的一些全局配置
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        //设置启动的一些配置
        /*
        主要启动方法，方法内部先封装了服务启动者信息ServiceConfig，
         在配置了应用信息ApplicationConfig， 最后将这些信息前部注册进DubboBootstrap中，
         DubboBootstrap是dubbo最新的启动类。
         */
        bootstrap.application(new ApplicationConfig("dubbo-demo-api-provider"))
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                //registry方法就是将注册中心信息放入configManager中
                .service(service)
                //service方法是将提供者信息放入configManager
                .start()
                /**bootstrap启动方法，从启动中可以看到十分重要的有四个类：
                      1、initialize(); 初始化
                      2、exportServices(); 暴露服务
                      3、exportMetadataService和registerServiceInstance 注册本地服务
                      4、referServices 订阅服务
                 */
                .await();
                //线程等待
                /*补充知识点：
                    wait()是Object类提供的，一般与synchronized联合使用。调用wait之后会释放锁，
                    导致线程等待。唤醒进程使用notify()或者notifyAll()。
                    await()Condition类是当中的，一般与Lock联合使用，
                    而Lock则是由Lock控制锁，Condition来控制被阻塞线程。
                 注意：此地的await是包装方法，使用的是与lock联合使用的await方法，
                      这里启用start后直接等待线程，可使方法一直置为启动。
                */
    }
    //兼容以前的启动方法，最新的已经不推荐使用这种方式
    private static void startWithExport() throws InterruptedException {
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        service.setApplication(new ApplicationConfig("dubbo-demo-api-provider"));
        service.setRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
        service.export();

        System.out.println("dubbo service started");
        new CountDownLatch(1).await();
    }
}
