package org.apache.spi;

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * @Author: Hanlei
 * @Date: 2021/3/1 10:26 上午
 */
public class DubboSPITest {

    @Test
    public void sayHello() throws Exception {

        ExtensionLoader<Robot> extensionLoader =
                ExtensionLoader.getExtensionLoader(Robot.class);
        Robot optimusPrim = extensionLoader.getAdaptiveExtension();
        Robot optimusPrime = extensionLoader.getExtension("optimusPrime");
        optimusPrime.sayHello();
        Robot bumblebee = extensionLoader.getExtension("bumblebee");
        bumblebee.sayHello();
    }

    private static void sorter(int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            for (int j = 0; j < array.length - i - 1; j++) {
                if (array[j] > array[j + 1]) {
                    int temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                }
            }
        }
    }

    @Test
    public void sort() throws Exception {
        /*Scanner sc = new Scanner(System.in);
        int count = sc.nextInt();*/
        int[]array = new int[]{5,4,2,3,1,6,7};
        /*for (int i=0;i<array.length;i++){
            array[i] = sc.nextInt();
        }*/
        System.out.println(Arrays.toString(array));
        sorter(array);
        System.out.println(Arrays.toString(array));
    }
    @Test
    public void sortBy() throws Exception {
        Integer N = 5;
        String newSort = "bdceafghijklmnopqrstuvwxyz";
        String N1 = "abcde";
        String N2 = "abc";
        String N3 = "cda";
        String N4 = "cad";
        String N5 = "ddc";
        System.out.println("输入：");
        System.out.println(N);
        System.out.println(newSort);
        System.out.println(N2);
        System.out.println(N3);
        System.out.println(N4);
        System.out.println(N5);
        System.out.println(N1);

        char[] newStringList = newSort.toCharArray();
        List<char[]> sortList = new ArrayList<>();
        sortList.add(N1.toCharArray());
        sortList.add(N2.toCharArray());
        sortList.add(N3.toCharArray());
        sortList.add(N4.toCharArray());
        sortList.add(N5.toCharArray());
        /*********排序算法***********/
        this.sortManage(N, newStringList, sortList);
        /*********排序算法***********/
        System.out.println("输出：");
        for (int i = 0; i <= sortList.size() - 1; i++) {
            System.out.println(Arrays.toString(sortList.get(i)));
        }
    }

    private void sortManage(Integer n, char[] newStringList, List<char[]> sortList) {
        List<int[]> stringSortList = new ArrayList<>();
        for (int i = 0; i <= n - 1; i++) {
            int[] strings = new int[50];
            //处理默认值为9 倒排时元素无数据默认下标为9或99
            for (int v=0;v<=49;v++) {
                strings[v] = 9;
            }
            for (int k = 0; k <= sortList.get(i).length - 1; k++) {
                for (int j = 0; j <= 25; j++) {
                    char c = newStringList[j];
                    if (c == sortList.get(i)[k]) {
                        //取出新顺序字母下标
                        strings[k] = Integer.valueOf(j);
                    }
                }
            }
            stringSortList.add(strings);
        }
        Map map = new HashMap();
        for (int l = 0; l <= stringSortList.size() - 1; l++) {
            String buffer = null;
            for (int s : stringSortList.get(l)) {
                if (buffer == null) {
                    buffer = String.valueOf(s);
                } else {
                    buffer = buffer + s;
                }
            }
            map.put(l, buffer);
        }
        System.out.println(map);
        sortList.sort(Comparator.comparing(c -> String.valueOf(map.get(sortList.indexOf(c)))));
    }

    @Test
    public void sorts() throws Exception {
        Integer N = 5;
        String newSort = "bdceafghijklmnopqrstuvwxyz";
        String N1 = "abcde";
        String N2 = "abc";
        String N3 = "cda";
        String N4 = "cad";
        String N5 = "ddc";
        System.out.println("输入：");
        System.out.println(N);
        System.out.println(newSort);
        System.out.println(N2);
        System.out.println(N3);
        System.out.println(N4);
        System.out.println(N5);
        System.out.println(N1);

        char[] newStringList = newSort.toCharArray();
        List<char[]> sortList = new ArrayList<>();
        sortList.add(N1.toCharArray());
        sortList.add(N2.toCharArray());
        sortList.add(N3.toCharArray());
        sortList.add(N4.toCharArray());
        sortList.add(N5.toCharArray());
        /*********排序算法***********/
        List<int[]> stringSortList = new ArrayList<>();
        for (int i = 0; i <= N - 1; i++) {

        }
    }
}
