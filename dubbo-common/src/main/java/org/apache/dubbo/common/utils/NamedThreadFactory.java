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
package org.apache.dubbo.common.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * InternalThreadFactory.
 */
public class NamedThreadFactory implements ThreadFactory {

    //前缀
    protected static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

    protected final AtomicInteger mThreadNum = new AtomicInteger(1);
    /**线程前缀递增计数器*/
    protected final String mPrefix;
    /**线程前缀*/
    protected final boolean mDaemon;
    /**是否是精灵线程（守护线程）*/
    protected final ThreadGroup mGroup;

    public NamedThreadFactory() {
        this("pool-" + POOL_SEQ.getAndIncrement(), false);
    }

    public NamedThreadFactory(String prefix) {
        this(prefix, false);
    }

    public NamedThreadFactory(String prefix, boolean daemon) {
        mPrefix = prefix + "-thread-";
        mDaemon = daemon;
        SecurityManager s = System.getSecurityManager();
        //安全管理器的应用场合，一般用在测试未知的且认为有恶意的程序
        /*当运行未知的Java程序的时候，该程序可能有恶意代码（删除系统文件、重启系统等），为了防止运行恶意代码对系统产生影响，
            需要对运行的代码的权限进行控制，这时候就要启用Java安全管理器。
        默认的安全管理器配置文件是 $JAVA_HOME/jre/lib/security/java.policy，即当未指定配置文件时，将会使用该配置*/
        mGroup = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = mPrefix + mThreadNum.getAndIncrement();
        Thread ret = new Thread(mGroup, runnable, name, 0);
        ret.setDaemon(mDaemon);
        /**是否是精灵线程（守护线程/后台线程）*/
        return ret;
    }

    public ThreadGroup getThreadGroup() {
        return mGroup;
    }
}
