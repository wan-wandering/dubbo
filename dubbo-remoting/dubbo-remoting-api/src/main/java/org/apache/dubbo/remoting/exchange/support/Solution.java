package org.apache.dubbo.remoting.exchange.support;

import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author: Hanlei
 * @Date: 2021/5/25 2:44 下午
 */
public class Solution {
    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode root = new ListNode(0);
        ListNode cursor = root;
        int carry = 0;
        while(l1 != null || l2 != null || carry != 0) {
            int l1Val = l1 != null ? l1.val : 0;
            int l2Val = l2 != null ? l2.val : 0;
            int sumVal = l1Val + l2Val + carry;
            carry = sumVal / 10;

            ListNode sumNode = new ListNode(sumVal % 10);
            cursor.next = sumNode;
            cursor = sumNode;

            if(l1 != null) l1 = l1.next;
            if(l2 != null) l2 = l2.next;
        }

        return root.next;
    }

    public static int lengthOfLongestSubstring(String s) {
        char[] ch = s.toCharArray();
        int i=0;
        int length =1;
        int flag = 1;
        int temp = 0;
        List<Integer> lengths = new ArrayList<>();
        for (int j=1;j<=ch.length;j++) {
            if (j<ch.length) {
                i = j;
                while (i > temp) {
                    if (ch[i - 1] != ch[j]) {
                        flag = 0;
                    } else {
                        lengths.add(length);
                        temp = temp + length;
                        length = 1;
                        flag = 1;
                        if (j == ch.length - 1) {
                            lengths.add(length);
                        }
                    }
                    i--;
                }
                if (flag == 0) {
                    length++;
                }
            } else {
                lengths.add(length);
            }
        }
        for (Integer ll: lengths) {
            if (length< ll) {
                length = ll;
            }
        }
        if (ch.length == 0) {
            length = 0;
        }
        return length;
    }
    public static int lengthOfLongestSubstrings(String s) {
        //滑动窗口
        // 记录字符上一次出现的位置
        int[] last = new int[128];
        for(int i = 0; i < 128; i++) {
            last[i] = -1;
        }
        int n = s.length();

        int res = 0;
        int start = 0; // 窗口开始位置
        for(int i = 0; i < n; i++) {
            int index = s.charAt(i);
            start = Math.max(start, last[index] + 1);
            res   = Math.max(res, i - start + 1);
            last[index] = i;
        }

        return res;
    }

    public static void main(String[] args) {
        String s = "";
        int l = lengthOfLongestSubstring(s);
        System.out.println(l);
    }
}
