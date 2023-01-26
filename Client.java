import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


class WriteData implements Runnable {

    private DataOutputStream out;

    private DataInputStream in;

    private ReentrantLock l;
    private Condition c;


    private BufferedReader inStdin = new BufferedReader(new InputStreamReader(System.in));


    WriteData(DataOutputStream out, DataInputStream in, ReentrantLock l, Condition c) {
        this.out = out;
        this.in = in;
        this.l = l;
        this.c = c;
    }


    private void sendCoords() throws IOException {
        out.writeInt(Integer.parseInt(inStdin.readLine()));
        out.flush();
        out.writeInt(Integer.parseInt(inStdin.readLine()));
        out.flush();
    }

    private void accountDispatch() throws IOException {
        System.out.println(in.readUTF());
        String username = inStdin.readLine();
        out.writeUTF(username);
        out.flush();
        System.out.println(in.readUTF());
        String password = inStdin.readLine();
        out.writeUTF(password);
        out.flush();
    }


    @Override
    public void run() {

        try {

            System.out.println(in.readUTF());

            try {
                l.lock();
                boolean login = false;
                while (!login) {
                    System.out.println(in.readUTF());
                    String choice = inStdin.readLine();
                    out.writeUTF(choice);
                    out.flush();
                    if (choice.equals("Registar")) {
                        accountDispatch();
                        System.out.println(in.readUTF());
                    } else if (choice.equals("Login")) {
                        accountDispatch();
                        login = in.readBoolean();
                        if (login) break;
                        else System.out.println(in.readUTF());
                    } else if (choice.equals("Exit")) {
                        return;
                    }
                }
            } finally {
                c.signal();
                l.unlock();
            }

            while (true) {

                String choice = inStdin.readLine();
                out.writeUTF(choice);
                out.flush();
                if (choice.equals("Login") || choice.equals("Registar")) {
                    String username = inStdin.readLine();
                    out.writeUTF(username);
                    out.flush();
                    String password = inStdin.readLine();
                    out.writeUTF(password);
                    out.flush();
                } else if (choice.equals("1")) {
                    sendCoords();
                } else if (choice.equals("2")) {
                    sendCoords();
                } else if (choice.equals("3")) {
                    sendCoords();
                } else if (choice.equals("4")) {
                    sendCoords();
                    out.writeInt(Integer.parseInt(inStdin.readLine()));
                    out.flush();
                } else if (choice.equals("5")) {
                    sendCoords();
                } else if (choice.equals("6")) {
                    //Client doesn't need to do anything
                } else if (choice.equals("Exit")) {
                    return;
                } else if (choice.equals("clear")) {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}


class ReadData implements Runnable {


    private DataInputStream in;

    private ReentrantLock l;
    private Condition c;


    ReadData(DataInputStream in, ReentrantLock l, Condition c) {
        this.in = in;
        this.l = l;
        this.c = c;
    }


    @Override
    public void run() {

        try {
            l.lock();
            boolean waiting = true;
            while (waiting) {
                c.await();
                waiting = false;
            }
            System.out.print("\033[H\033[2J");
            System.out.flush();
            while (true) {
                try {
                    System.out.println(in.readUTF());
                }
                catch (IOException e){
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            l.unlock();
        }

    }


}


public class Client {


    public static void main(String[] args) throws IOException, InterruptedException {

        Socket socket = new Socket("localhost", 12345);

        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        ReentrantLock l = new ReentrantLock();
        Condition c = l.newCondition();


        Thread writeData = new Thread(new WriteData(out, in, l, c));
        Thread readData = new Thread(new ReadData(in, l, c));

        writeData.start();
        readData.start();

        writeData.join();
        //readData.join();

        System.out.println("Terminando Programa...");

        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
    }

}


