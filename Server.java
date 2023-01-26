import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;



 class ServerWorker implements Runnable {

    private String bemVindo() {
        return "  ____                    __      ___           _       \n" +
                " |  _ \\                   \\ \\    / (_)         | |      \n" +
                " | |_) | ___ _ __ ___ _____\\ \\  / / _ _ __   __| | ___  \n" +
                " |  _ < / _ \\ '_ ` _ \\______\\ \\/ / | | '_ \\ / _` |/ _ \\ \n" +
                " | |_) |  __/ | | | | |      \\  /  | | | | | (_| | (_) |\n" +
                " |____/ \\___|_| |_| |_|       \\/   |_|_| |_|\\__,_|\\___/ \n";

    }

    private String mostraOpcoes() {
        return "Pressione 1 para Procurar Trotinete!\n" +
                "Pressione 2 para Procurar Recompensas\n" +
                "Pressione 3 para Reservar Trotinete\n" +
                "Pressione 4 para Estacionar Trotinete!\n" +
                "Pressione 5 para ser notificado de Recompensas!\n" +
                "Pressione 6 para deixar de ser notificado de Recompensas!\n" +
                "Exit para Sair";
    }

    private final Socket socket;
    private final ServerManager manager;

    public ServerWorker(Socket socket, ServerManager manager) {
        this.socket = socket;
        this.manager = manager;
    }

    private Point getCoords(DataInputStream in, DataOutputStream out) throws IOException {
        out.writeUTF("Coordenada X:");
        out.flush();
        int x = in.readInt();
        out.writeUTF("Coordenada Y:");
        out.flush();
        int y = in.readInt();
        return new Point(x,y);
    }


    public void run() {

        try {

            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            out.writeUTF(bemVindo());

            String usernameLogged = null;

            boolean login = false;
            while (!login) {
                out.writeUTF("Login ou Registar? Exit para sair!");
                out.flush();
                String choice = in.readUTF();
                if (choice.equals("Exit")) {
                    socket.shutdownInput();
                    socket.shutdownOutput();
                    socket.close();
                    return;
                }
                if (choice.equals("Registar")) {
                    out.writeUTF("Username:");
                    out.flush();
                    String username = in.readUTF();
                    out.writeUTF("Password:");
                    out.flush();
                    String password = in.readUTF();
                    try {
                        manager.registerUser(username, password);
                        out.writeUTF("Registo completo!");
                        out.flush();
                    } catch (Exception e) {
                        out.writeUTF(e.getMessage());
                        out.flush();
                    }
                }
                else if (choice.equals("Login")) {
                    out.writeUTF("Username:");
                    out.flush();
                    String username = in.readUTF();
                    out.writeUTF("Password:");
                    out.flush();
                    String password = in.readUTF();
                    try {
                        manager.loginUser(username, password);
                        login = true;
                        usernameLogged = username;
                        out.writeBoolean(true);
                        out.writeUTF("Login efetuado!");
                        out.flush();
                    } catch (Exception e) {
                        out.writeBoolean(false);
                        out.writeUTF(e.getMessage());
                        out.flush();
                    }
                }
            }

            Thread notifications = null;
            NotificationManager notificationManager = null;

            while (true) {
                out.writeUTF(mostraOpcoes());
                out.flush();

                String choice = in.readUTF();

                if (choice.equals("1")) { //procurar trotinete
                    Point p = getCoords(in,out);
                    List<Point> trotinetes_spots = manager.listFreeTrotinetesOnCoords(p);
                    if (trotinetes_spots.size() == 0) out.writeUTF("Não existem Trotinetes perto desta área!");
                    else out.writeUTF(trotinetes_spots.toString());
                    out.flush();
                }
                if (choice.equals("2")) {
                    Point p = getCoords(in,out);
                    List<Reward> rewards_spot = ServerManager.listRewardsOnCoords(p);
                    if (rewards_spot.size() == 0) out.writeUTF("Não existem Recompensas perto desta área!");
                    else out.writeUTF(rewards_spot.toString());
                    out.flush();
                }
                if (choice.equals("3")) {
                    Point p = getCoords(in,out);
                    String answer = manager.reservaTrotinete(p, usernameLogged);
                    out.writeUTF(answer);
                    out.flush();
                }
                if (choice.equals("4")) {
                    Point p = getCoords(in,out);
                    out.writeUTF("Código da Reserva");
                    int code = in.readInt();
                    try {
                        double price = manager.estacionaTrotinete(p, usernameLogged, code);
                        if (price <= 0) out.writeUTF("Custo da viagem: " + Math.abs(price) + "$");
                        else out.writeUTF("Recompensa da viagem: " + price + "$");
                        out.flush();
                    } catch (Exception e) {
                        out.writeUTF(e.getMessage());
                        out.flush();
                    }
                }
                if (choice.equals("5")) { //notificacao
                    Point p = getCoords(in,out);
                    if (notificationManager == null) {
                        notificationManager = new NotificationManager(out, manager, p);
                        notifications = new Thread(notificationManager);
                        notifications.start();
                        manager.subscribe(notificationManager);
                        out.writeUTF("Subscrição feita");
                        out.flush();
                    }
                    else out.writeUTF("Erro na subscrição!");
                    out.flush();
                }
                if (choice.equals("6")) { //desligar notificacao
                    if (notificationManager != null) {
                        manager.unsubscribe(notificationManager);
                        notifications.interrupt();
                        notifications = null;
                        notificationManager = null;
                        out.writeUTF("Subscrição anulada");
                    }
                    else out.writeUTF("Erro na anulação da subscrição!");
                    out.flush();
                }
                if (choice.equals("Exit")) {
                    manager.exit(usernameLogged, notificationManager);
                    if (notifications != null) notifications.interrupt();
                    socket.shutdownInput();
                    socket.shutdownOutput();
                    socket.close();
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}

public class Server {

    private static final Executor threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(12345);
        ServerManager manager = new ServerManager();

        Thread rewards = new Thread(new RewardManager(manager));
        rewards.start();


        while (true) {
            Socket socket = serverSocket.accept();
            threadPool.execute(new ServerWorker(socket, manager));
        }

    }


}

