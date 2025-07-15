import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

class ClientHandle extends Thread {
    private Socket socket;
    private String folderPath;
    private String clientName;

    ClientHandle(Socket socket, String folderPath, String clientName) {
        this.socket = socket;
        this.folderPath = folderPath;
        this.clientName = clientName;
    }

    private void sendFileList(DataOutputStream dos) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();

        try {
            dos.writeInt(files.length);

            for (File file : files) {
                dos.writeUTF(file.getName());
            }

        } catch (Exception ex) {
            System.err.println("Error sending file list: " + ex.getMessage());
        }
        
    }

    @Override
    public void run() {
        try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        		DataInputStream dis = new DataInputStream(socket.getInputStream());) {

            while (true) {
                sendFileList(dos);
                String[] fileName = dis.readUTF().split("\\.");

                File file = new File(folderPath + fileName[0] + "." + fileName[1]);
                long bytesToCopy = file.length();
                dos.writeLong(bytesToCopy);

                zeroCopy(folderPath, fileName, bytesToCopy, dos);
                basicCopy(folderPath, fileName, bytesToCopy, dos);
            }

        } catch (IOException ex) {
            System.out.println("Server: " + clientName + " disconnected.");
        }
    }


    private static void basicCopy(String from, String[] fileName, long bytesToCopy, DataOutputStream dos) throws IOException {
    	byte[] data = new byte[8 * 1024];
        long bytesCopied = 0;
 
    	try (FileInputStream fis = new FileInputStream(from + fileName[0] + "." +fileName[1])) {
    		long start = System.currentTimeMillis();
            
    		int bytesRead;
    		while (bytesCopied < bytesToCopy && (bytesRead = fis.read(data)) != -1) {
                dos.write(data, 0, bytesRead);
                bytesCopied += bytesRead;
            }
            dos.flush();
    		
            long end = System.currentTimeMillis();
            System.out.println("\t[Basic Copy]>> Time " + (end - start) + " ms.");
    	} catch (Exception ex) {
    		 System.err.println("Error in basicTimeCounter: " + ex.getMessage());
    	}
    	
    }

    private static void zeroCopy(String from, String[] fileName, long bytesToCopy, DataOutputStream dos) throws IOException {
    	
    	FileChannel src = null;
    	
    	try (FileInputStream fis = new FileInputStream(from + fileName[0] + "." + fileName[1])) {
    		long start = System.currentTimeMillis();
    		
    		src = fis.getChannel();
    	
    		long position = 0;
            while (position < bytesToCopy) {
            	position += src.transferTo(position, bytesToCopy, Channels.newChannel(dos));
            }
            dos.flush();
            
            long end = System.currentTimeMillis();
            System.out.println("\t[Zero Copy]>> Time " + (end - start) + " ms.");
    	} catch (Exception ex) {
    		 System.err.println("Error in basicTimeCounter: " + ex.getMessage());
    	}
    }
}


public class Server {
	private static final int serverPort = 3000;
	private static final String folderPath = "C:/Users/Pasut/Desktop/Source/";
	private static int clientCount = 0;

	public static void main(String[] args) {
		try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
			System.out.println("Server starting connection...");

			while (true) {
				Socket socket = serverSocket.accept();
				clientCount++;
				System.out.println("Server: <Client" + clientCount + "> connected!");
				new ClientHandle(socket, folderPath, "Client".concat(Integer.toString(clientCount))).start();
			}

		} catch (IOException ex) {
			System.out.println("Cannot listen on Port: " + serverPort);
			System.exit(-1);
		}
	}
}