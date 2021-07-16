package org.apache.dubbo.remoting.exchange.codec;

/**
 * @Author: Hanlei
 * @Date: 2021/6/2 2:46 下午
 */

public class TestThread implements Runnable{
    private String name;
    private Object pre;
    private Object self;

    public TestThread(String name,Object pre,Object self){
        this.name = name;
        this.pre = pre;
        this.self = self;
    }
    @Override
    public void run() {
        int count = 1;
        while(count > 0){
            synchronized (pre) {
                synchronized (self) {
                    System.out.println(name);
                    count--;
                    self.notifyAll();
                }
                try {
                    if (count == 0) {
                        pre.notifyAll();
                    } else {
                        pre.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        Object a = new Object();
        Object b = new Object();
        Object c = new Object();
        TestThread A = new TestThread("A",c,a);
        TestThread B = new TestThread("B",a,b);
        TestThread C = new TestThread("C",b,c);
        new Thread(A).start();
        try {
            Thread.sleep(10);//保证初始ABC的启动顺序
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(B).start();
        try {
            Thread.sleep(10);//保证初始ABC的启动顺序
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(C).start();
        try {
            Thread.sleep(10);//保证初始ABC的启动顺序
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
