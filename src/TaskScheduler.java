import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by wlf on 2016/11/8.
 */
public class TaskScheduler {

    private int threadNumber;

    private String urlStr;

    private String fileName;

    private ExecutorService executor;

    private long fileLength;

    static Map<Integer, Double> processing = new HashMap<>();

    static volatile int doneNumber = 0;


    public TaskScheduler(int threadNumber, String url) {
        this.threadNumber = threadNumber;
        this.urlStr = url;
        executor = new ScheduledThreadPoolExecutor(threadNumber);
    }

    public void download() throws Exception {
        getFileMeta();
        long startPosition = 0;
        long length = fileLength / threadNumber;
        for (int i = 0; i < threadNumber; i++) {
            length = startPosition + length > fileLength ? fileLength - startPosition : length;
            DownloadTask task = new DownloadTask(urlStr, i, startPosition, length);
            executor.execute(task);
            startPosition = startPosition + length + 1;
        }
        new ProccesorIndicator(this).run();
    }

    public void getFileMeta() throws Exception {

        URL URL = new URL(urlStr);

        HttpURLConnection conn = (HttpURLConnection) URL.openConnection();

        conn.setRequestProperty("Accept-Encoding", "identity");

        conn.connect();

        fileLength = conn.getContentLength();


        if (fileName == null) {
            String[] strings = urlStr.split("/");
            fileName = strings[strings.length - 1];
            fileName = fileName.trim();
        }

        conn.disconnect();
    }

    public void merge() throws Exception {
        System.out.println("\nmerging");
        File outPutFile = new File(fileName);
        FileOutputStream output = new FileOutputStream(outPutFile);
        byte[] buffer = new byte[1024];
        for (int i = 0; i < threadNumber; i++) {
            File file = new File(i + ".tmp");
            InputStream is = new FileInputStream(file);
            while (true) {
                int len = is.read(buffer);
                if (len == -1) {
                    break;
                }
                output.write(buffer, 0, len);
            }
            is.close();
            file.delete();
        }
        System.out.println("Download finish " + fileName);
        output.flush();
        output.close();
    }

    static void update(Integer number, Double rate) {
        processing.put(number, rate);
    }

    static synchronized void finishTask() {
        doneNumber++;
    }

    class ProccesorIndicator implements Runnable {
        TaskScheduler taskScheduler;

        public ProccesorIndicator(TaskScheduler taskScheduler) {
            this.taskScheduler = taskScheduler;
        }

        @Override
        public void run() {
            try {
                double whole = 0;
                while (true) {
                    System.out.print("\r");
                    System.out.print("[");
                    long hasDownload = (long) (whole /threadNumber * fileLength);
                    whole = 0;
                    for (Integer i : processing.keySet()) {
                        printSharp(processing.get(i), threadNumber);
                        whole += processing.get(i);
                    }
                    System.out.print("]");
                    System.out.printf("\t total: %.2f %%\t %d/%d \t %d KB/s",
                            100 * whole / threadNumber,
                            (long) (whole * fileLength / threadNumber), fileLength,
                            (long) (whole * fileLength / threadNumber - hasDownload) / 1024);
                    if (doneNumber == threadNumber) {
                        taskScheduler.merge();
                        break;
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void printSharp(double rate, int threadNumber) {
        int totalSharp = 60 / threadNumber;
        int out = (int) (totalSharp * rate);
        for (int i = 0; i < out; i++) {
            System.out.print("#");
        }
        for (int i = 0; i < totalSharp - out; i++) {
            System.out.print(" ");
        }
    }
}
