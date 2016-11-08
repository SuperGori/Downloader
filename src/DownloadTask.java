import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Created by wlf on 2016/11/8.
 */
public class DownloadTask implements Runnable {

    private String Url;

    private int number;

    private long startPosition;

    private long length;

    public DownloadTask(String url, int number, long startPosition, long length) {
        Url = url;
        this.number = number;
        this.startPosition = startPosition;
        this.length = length;
    }

    @Override
    public void run() {
        try {
            URL url = new URL(Url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("range", "bytes=" + startPosition + "-" + (startPosition + length));
            conn.connect();
            InputStream is = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(number + ".tmp");
            byte[] buffer = new byte[1024];
            int downloaded = 0;
            while (true) {
                int len = is.read(buffer);
                if (len == -1) {
                    break;
                }
                fos.write(buffer, 0, len);
                downloaded += len;
                TaskScheduler.update(number, downloaded * 1.0 / length);
            }
            fos.flush();
            fos.close();
            conn.disconnect();
            TaskScheduler.finishTask();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
