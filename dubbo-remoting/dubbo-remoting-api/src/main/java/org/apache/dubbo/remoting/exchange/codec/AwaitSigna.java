package org.apache.dubbo.remoting.exchange.codec;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: Hanlei
 * @Date: 2021/6/2 5:05 下午
 */
public class AwaitSigna{


    public static void main(String[] args) throws InterruptedException{
        Await await = new Await();
        Condition a = await.newCondition();
        Condition b = await.newCondition();
        Condition c = await.newCondition();
        new Thread(()->{
            await.println("A", a, b);
        }).start();
        new Thread(()->{
            await.println("B", b, c);
        }).start();
        new Thread(()->{
            await.println("C", c, a);
        }).start();

        TimeUnit.SECONDS.sleep(1);
        await.lock();
        try {
            a.signal();
        } finally {
            await.unlock();
        }
    }
}
class Await extends ReentrantLock {
    public Await(){}
    public void println(String str, Condition curr, Condition next) {
        for (int i = 0; i < 5; i++) {
            lock();
            try {
                curr.await();
                System.out.println(str);
                next.signal();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                unlock();
            }

        }
    }
}
