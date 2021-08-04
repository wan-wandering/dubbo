package org.apache.dubbo.remoting.exchange.support;


/**
 * @Author: Hanlei
 * @Date: 2021/7/22 4:27 下午
 */
public class MergeListNode {
    public static ListNode mergeTwoListNode(ListNode l1, ListNode l2) {
        if (l1 == null) {
            return l2;
        }
        if (l2 == null) {
            return l1;
        }
        ListNode head = new ListNode(0);
        if (l1.val <= l2.val) {
            head = l1;
            head.next = mergeTwoListNode(l1.next, l2);
        } else {
            head = l2;
            head.next = mergeTwoListNode(l1, l2.next);
        }
        return head;
    }

    public static void main(String[] args) {
        ListNode l1 = new ListNode();
        l1.val = 1;
        l1.next = new ListNode(2);

        ListNode l2 = new ListNode();
        l2.val = 1;
        l2.next = new ListNode(3);
        //递归方法
        System.out.println("递归");
        ListNode merge1 = mergeTwoListNode(l1, l2);
        while (merge1 != null) {
            System.out.println(merge1.val);
            merge1 = merge1.next;
        }
    }
}
