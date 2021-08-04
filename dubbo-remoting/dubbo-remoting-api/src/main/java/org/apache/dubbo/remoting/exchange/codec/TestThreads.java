package org.apache.dubbo.remoting.exchange.codec;

public class TestThreads implements Runnable{
  private String name;
  private Object pre;
  private Object self;
  public TestThreads (String name, Object pre, Object self){
      this.name = name;
      this.pre = pre;
      this.self = self;
  }

    @Override
    public void run() {
        int count = 5;
        while (count>0) {
            synchronized (pre) {
                synchronized (self) {
                    System.out.println(name);
                    count--;
                    self.notifyAll();
                }
                try {
                    pre.wait();
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
        TestThreads testThreadsA = new TestThreads("A", c, a);
        TestThreads testThreadsB = new TestThreads("B", a, b);
        TestThreads testThreadsC = new TestThreads("C", b, c);
        new Thread(testThreadsA).start();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(testThreadsB).start();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }new Thread(testThreadsC).start();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}