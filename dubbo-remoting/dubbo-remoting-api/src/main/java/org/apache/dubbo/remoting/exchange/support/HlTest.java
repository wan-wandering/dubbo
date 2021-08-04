package org.apache.dubbo.remoting.exchange.support;

/**
 * @Author: Hanlei
 * @Date: 2021/7/22 2:27 下午
 */
public class HlTest implements Runnable {
    private String name;
    private Object pre;
    private Object self;
    private static final String STRC = "C";

    public HlTest(String name, Object pre, Object self) {
        this.name = name;
        this.pre = pre;
        this.self = self;
    }

    @Override
    public void run() {
        int count = 5;
        while (count > 0) {
            synchronized (pre) {
                synchronized (self) {
                    System.out.println(name);
                    if (name.equals(STRC)) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
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
        HlTest hlTestA = new HlTest("A", c, a);
        HlTest hlTestB = new HlTest("B", a, b);
        HlTest hlTestC = new HlTest("C", b, c);
        new Thread(hlTestA).start();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(hlTestB).start();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(hlTestC).start();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
