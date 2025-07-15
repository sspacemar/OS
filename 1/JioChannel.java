import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class JioChannel {
    public static void main(String[] args) {
        JioChannel channel = new JioChannel();
        try {
            if (args.length < 3) {
                System.out.println("usage: JioChannel <source> <destination> <mode>\n");
                return;
            }
            if ("l".equals(args[2])){
                long start = System.currentTimeMillis();
                channel.copy(args[0], args[1]);
                long end = System.currentTimeMillis();
                long time = end - start;
                System.out.println("Time " + time + " millisecond");
            } else {
                long start = System.currentTimeMillis();
                channel.zeroCopy(args[0],args[1]);
                long end = System.currentTimeMillis();
                long time = end - start;
                System.out.println("Time " + time + " millisecond");

            }
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
    public void copy(String from, String to) throws IOException {
        byte[] data = new byte[8 * 1024];
        FileInputStream fis = null;
        FileOutputStream fos = null;
        long bytesToCopy = new File(from).length();
        long bytesCopied = 0;
        try {
            fis = new FileInputStream(from);
            fos = new FileOutputStream(to);

            while (bytesCopied < bytesToCopy) {
                fis.read(data);
                fos.write(data);
                bytesCopied += data.length;
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }
    public void zeroCopy(String from, String to) throws IOException {

        FileChannel source = null;
        FileChannel destination = null;
        try{
            source = new FileInputStream(from).getChannel();
            destination = new FileOutputStream(to).getChannel();
            source.transferTo(0, source.size(), destination);

        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }
}