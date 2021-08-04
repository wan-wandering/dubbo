package org.apache.dubbo.remoting.exchange.codec;

import com.google.common.collect.Lists;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.dubbo.common.utils.StringUtils;

import javax.swing.tree.TreeNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @Author: Hanlei
 * @Date: 2021/6/6 9:56 上午
 */
public class HanLeiTest {

        public static void main(String[] args) throws IOException {
            long startTime = System.currentTimeMillis();

            statistics();

            long endTime = System.currentTimeMillis();

            System.out.println("execute time " + (endTime - startTime) + " ms.");
        }


        public static void statistics() throws IOException {
            System.out.println("请输入搜索关键词(多个关键词以英文逗号隔开):");
            Scanner scanner = new Scanner(System.in);
            String keyWord = scanner.nextLine();
            String [] keyWords = keyWord.split(",");
            File file = new File("/Users/wb-hl818878/work/Enum.txt");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            Map<String, Integer> resultMap = new HashMap<>();
            while((line = bufferedReader.readLine()) != null){

                for (String kw : keyWords) {
                    int count = line.split(kw).length - 1;
                    if (count == 0 && line.endsWith(kw)) {
                        count = 1;
                    }
                    if (resultMap.containsKey(kw)) {
                        resultMap.put(kw, resultMap.get(kw) + count);
                    } else {
                        resultMap.put(kw, count);
                    }
                }
            }
            List<Map.Entry<String, Integer>> list = Lists.newArrayList(resultMap.entrySet());
            list.sort((o1, o2) -> o2.getValue() - o1.getValue());
            System.out.println("result: ");
            for (Map.Entry entry : list) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
    }
