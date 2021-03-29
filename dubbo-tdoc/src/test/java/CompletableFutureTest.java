import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Author: Hanlei
 * @Date: 2020/12/23 7:45 下午
 */
public class CompletableFutureTest {
    public static void main(String[] args) {
        CompletableFuture<String> future = new CompletableFuture<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("CompletableFutureTest 监控任务执行");
                future.complete("任务查看结果");
            }
        }).start();
        try {
            System.out.println(future.get(5000, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
