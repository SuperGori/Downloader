import java.util.Scanner;

/**
 * Created by wlf on 2016/11/8.
 */
public class UI {


    public static void main(String[] args) {
        try {
            initTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initTask() throws Exception{
        Scanner scanner = new Scanner(System.in);
        System.out.println("Input Url below :");
        String url = scanner.nextLine();
        System.out.println("Input threads number below");
        int threadNumber = scanner.nextInt();
        scanner.close();
        new TaskScheduler(threadNumber,url).download();
    }


}
