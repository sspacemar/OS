
import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;

public class Client {

    private static final String hostName = "192.168.170.57";
    private static final int serverPort = 3000;

    private static void receiveBasic(DataInputStream dis, String savePath, long fileSize) {
        long startTime = System.currentTimeMillis();

        try (FileOutputStream fos = new FileOutputStream(savePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;

            while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

        } catch (IOException e) {
            System.err.println("Error in receiveBasic: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Basic mode receive time: " + (endTime - startTime) + " ms");
    }

    private static void receiveZero(DataInputStream dis, String savePath, long fileSize) throws IOException {
        long startTime = System.currentTimeMillis();
        FileChannel dest = null;
        ReadableByteChannel src = null;

        try {
            dest = new FileOutputStream(savePath).getChannel();
            src = Channels.newChannel(dis);

            long totalTransferred = 0;
            long transferred;
            
            while (totalTransferred < fileSize) {
                transferred = dest.transferFrom(src, totalTransferred, fileSize - totalTransferred);
                if (transferred == 0) {
                break;
            }
            totalTransferred += transferred;
        }

        } catch (IOException e) {
            System.err.println("Error in receiveZero: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Zero-copy mode receive time: " + (endTime - startTime) + " ms");
    }

    public static void main(String[] args) {
        try (Socket socket = new Socket(hostName, serverPort); DataInputStream dis = new DataInputStream(socket.getInputStream()); DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); Scanner sc = new Scanner(System.in)) {

            while (!socket.isClosed()) {
                int totalFiles = dis.readInt();
                if (totalFiles == 0) {
                    System.out.println("System: No files available on the server.");
                } else {
                    System.out.println("=======[Available files]=======");
                    for (int i = 0; i < totalFiles; i++) {
                        String fileName = dis.readUTF();
                        System.out.println("[" + (i + 1) + "] " + fileName);
                    }
                    System.out.println("===============================");
                }

                System.out.print("[Input file name] >> ");
                String fileName = sc.nextLine();
                dos.writeUTF(fileName);

                String[] name = fileName.split("\\.");

                long fileSize = dis.readLong();
                
                String savePathZero = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + name[0] + "_FromZeroCopy." + name[1];
                receiveZero(dis, savePathZero, fileSize);

                String savePathBasic = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + name[0] + "_FromBasicCopy." + name[1];
                receiveBasic(dis, savePathBasic, fileSize);
                
                socket.close();
            }

        } catch (IOException ex) {
            System.err.println("Client error: " + ex.getMessage());
        }
    }
}
