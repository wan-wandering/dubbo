package org.apache.dubbo.remoting.transport.netty4;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: Hanlei
 * @Date: 2021/4/19 12:20 下午
 */
public class Sorts {


    /**
     * 冒泡排序
     *
     * @param array
     * @return
     */
    public static int[] bubbleSort(int[] array) {
        if (array.length == 0) {
            return array;
        }
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array.length - 1 - i; j++) {
                if (array[j + 1] < array[j]) {
                    int temp = array[j + 1];
                    array[j + 1] = array[j];
                    array[j] = temp;
                }
            }
        }
        return array;
    }

    /**
     * 冒泡排序
     *
     * @param array
     * @return
     */
    public static int[] bubble(int[] array) {
        if (array.length == 0) {
            return array;
        }
        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length - 1 - i; j++) {
                if (array[i] > array[j]) {
                    int temp = array[i];
                    array[i] = array[j];
                    array[j] = temp;
                }
            }
        }
        return array;
    }

    /**
     * 选择排序
     *
     * @param array
     * @return
     */
    public static int[] selectionSort(int[] array) {
        if (array.length == 0) {
            return array;
        }
        for (int i = 0; i < array.length; i++) {
            int minIndex = i;
            for (int j = i; j < array.length; j++) {
                if (array[j] < array[minIndex]) {
                    //找到最小的数
                    minIndex = j;
                    //将最小数的索引保存
                }
            }
            int temp = array[minIndex];
            array[minIndex] = array[i];
            array[i] = temp;
        }
        return array;
    }

    /**
     * 选择排序
     *
     * @param array
     * @return
     */
    public static int[] selection(int[] array) {
        if (array.length == 0) {
            return array;
        }
        for (int i = 0; i < array.length; i++) {
            int minIndex = i;
            for (int j = i; j < array.length; j++) {
                if (array[minIndex] < array[j]) {
                    minIndex = j;
                }
            }
            int temp = array[minIndex];
            array[minIndex] = array[i];
            array[i] = temp;
        }
        return array;
    }

    /**
     * 插入排序
     *
     * @param array
     * @return
     */
    public static int[] insertionSort(int[] array) {
        if (array.length == 0) {
            return array;
        }
        int current;
        for (int i = 0; i < array.length - 1; i++) {
            current = array[i + 1];
            int preIndex = i;
            while (preIndex >= 0 && current < array[preIndex]) {
                array[preIndex + 1] = array[preIndex];
                preIndex--;
            }
            array[preIndex + 1] = current;
        }
        return array;
    }

    /**
     * 插入排序
     *
     * @param array
     * @return
     */
    public static int[] insertion(int[] array) {
        if (array.length == 0) {
            return array;
        }
        int current;
        for (int i = 0; i < array.length; i++) {
            current = array[i + 1];
            int preIndex = i;
            while (preIndex > 0 && current > array[preIndex]) {
                array[preIndex + 1] = array[preIndex];
                preIndex--;
            }
            array[preIndex + 1] = current;
        }
        return array;
    }

    /**
     * 快速排序方法
     *
     * @param array
     * @param start
     * @param end
     * @return
     */
    public static int[] quickSort(int[] array, int start, int end) {
        if (array.length < 1 || start < 0 || end >= array.length || start > end) {
            return null;
        }
        //进行partition分区
        int smallIndex = partition(array, start, end);
        //递归进行
        if (smallIndex > start) {
            quickSort(array, start, smallIndex - 1);
        }
        if (smallIndex < end) {
            quickSort(array, smallIndex + 1, end);
        }
        return array;
    }

    /**
     * 快速排序算法——partition
     *
     * @param array
     * @param start
     * @param end
     * @return
     */
    public static int partition(int[] array, int start, int end) {
        int pivot = (int) (start + Math.random() * (end - start + 1));
        int smallIndex = start - 1;
        swap(array, pivot, end);
        for (int i = start; i <= end; i++) {
            if (array[i] <= array[end]) {
                smallIndex++;
                if (i > smallIndex) {
                    swap(array, i, smallIndex);
                }
            }
        }
        return smallIndex;
    }

    /**
     * 交换数组内两个元素
     *
     * @param array
     * @param i
     * @param j
     */
    public static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    /* public static void quicksort(int[] arr, int p, int q) {
         if (p >= q) {
             return;
         }
         int pivot = partion(arr, p, q);
         quicksort(arr, p, pivot - 1);
         // 左边有序
         quicksort(arr, pivot + 1, q);
         // 右边有序
     }
     public static int partion(int[] arr, int p, int q) {
         int pivot = arr[q];
         int pp = p;
         int qq = q;
         while (pp < qq) {
             while (pp < qq && arr[pp] <= pivot) {
                 pp++;
             }
             if (pp < qq) {
                 int tmp = arr[pp];
                 arr[pp] = arr[qq];
                 arr[qq] = tmp;
             }
             while (pp < qq && arr[qq] >= pivot) {
                 qq--;
             }
             if (pp < qq) {
                 int tmp = arr[pp];
                 arr[pp] = arr[qq];
                 arr[qq] = tmp;
             }
         }
         arr[pp] = pivot;
         return pp;
     }*/
    private static void SumRead() throws InterruptedException {
        Sorts sumThread = new Sorts();
        new Thread(()->{
            try {
                sumThread.one();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        TimeUnit.SECONDS.sleep(1);

        new Thread(()->{
            try {
                sumThread.two();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void one() throws InterruptedException{
        synchronized (this) {
            boolean flag = true;

            while (flag) {

                for(int i = 1; i <= 99;i += 2){
                    System.out.println("one" + i);

                    if(i==99){
                        flag = false;
                        this.notify();
                        break;
                    }
                    this.notify();
                    this.wait();
                }
            }
        }
    }

    public void two() throws InterruptedException{
        synchronized (this) {
            boolean flag = true;

            while (flag) {

                for(int i = 2; i <= 100;i += 2){
                    System.out.println("two" + i);

                    if(i==100){
                        flag = false;
                        this.notify();
                        break;
                    }
                    this.notify();
                    this.wait();
                }
            }
        }
    }

    private static volatile int start = 0;
    private static final Integer number = 10;

    public static void main(String[] args) throws InterruptedException {
        int[] a = {5,7,4,8,2,1,6};
        //insertion(a);
        quickSort(a,0, 6);
        //bubble(a);
        System.out.println(JSON.toJSONString(a));
        //ThreadIncre();
        //SumRead();

    }
    private static void ThreadIncre() throws InterruptedException {
        Lock lock = new ReentrantLock();

        CountDownLatch countDownLatch = new CountDownLatch(number);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        lock.lock();
                        if (start < 1000) {
                            start++;
                        } else {
                            break;
                        }

                    } finally {
                        lock.unlock();
                    }
                }
                countDownLatch.countDown();

            }, "Thread-1" + i);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        //countDownLatch.await();
        System.out.println(start);
    }


}
