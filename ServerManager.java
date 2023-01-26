import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


public class ServerManager {

    static class Trotinete {

        private boolean livre;
        private Point coordenadas;
        private LocalTime tempoReserva;

        public Trotinete(Point coordenadas) {
            livre = true;
            this.coordenadas = coordenadas;
            tempoReserva = null;
        }

        public boolean isLivre() {
            return livre;
        }

        public void reservaTrotinete() {
            livre = false;
            tempoReserva = LocalTime.now();
        }

        public void estacionaTrotinete(Point destino) {
            livre = true;
            coordenadas = destino;
            tempoReserva = null;
        }

        public double custoViagem(Point destino) {
            int distance = Point.manhattanDistance(coordenadas, destino);
            long minutes = Duration.between(tempoReserva, LocalTime.now()).toMinutes();
            return -(0.5 * (distance + minutes));
        }

        public double recompensaViagem(Point destino) {
            return 0.25 * Point.manhattanDistance(coordenadas, destino);
        }
    }


    static class User {

        private final String username, password;
        private boolean isLogged;
        private final List<Integer> codigosReservados;

        public User(String username, String password) {
            this.username = username;
            this.password = password;
            isLogged = false;
            codigosReservados = new ArrayList<>();
            //destinos = new ArrayList<>();
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public void login() {
            isLogged = true;
        }

        public void logoff() {
            isLogged = false;
        }

        public boolean isLogged() {
            return isLogged;
        }

        public void addCodigo(int code) {
            codigosReservados.add(code);
        }

        public void removeCodigo(int code) {
            codigosReservados.remove(Integer.valueOf(code));
        }

        public boolean containsCodigo(int code) {
            return codigosReservados.contains(code);
        }

        public List<Integer> getCodigosReservados() {
            return new ArrayList<>(codigosReservados);
        }

        public void deleteCodigosReservados(){
            codigosReservados.clear();
        }
    }

    static class TrotinetesMatrix {

        private ReentrantLock l = new ReentrantLock();

        private final List<Trotinete>[][] tm;

        public TrotinetesMatrix() {
            //Inicializa matriz de Trotinetes
            tm = new ArrayList[SIZE_MAP][SIZE_MAP];
            for (int i = 0; i < SIZE_MAP; i++) {
                for (int j = 0; j < SIZE_MAP; j++) {
                    tm[i][j] = new ArrayList<>();
                }
            }
            //Cria N Trotinetes espalhadas aleatoriamente pelo mapa
            Random random = new Random();
            for (int i = 0; i < NUMBER_TROTINETES; i++) {
                int x = random.nextInt(SIZE_MAP);
                int y = random.nextInt(SIZE_MAP);
                tm[x][y].add(new Trotinete(new Point(x, y)));
            }
        }
    }

    static class TrotinetesReservadasMap extends HashMap<Integer, Trotinete> {
        private final ReentrantLock l = new ReentrantLock();
        private int codeReserva = 0;
    }

    static class RewardsMap extends HashMap<Point, Reward> {
        private final ReentrantLock l = new ReentrantLock();

        public ReentrantLock getLock() {
            return l;
        }
    }

    static class UsersMap extends HashMap<String, User> {
        private final ReentrantLock l = new ReentrantLock();
    }

    static class NotificationManagerList extends ArrayList<NotificationManager> {
        private final ReentrantLock l = new ReentrantLock();
    }

    public static final int SIZE_MAP = 10;
    public static final int NUMBER_TROTINETES = 20;
    public static final int FIXED_DISTANCE = 2;

    private static TrotinetesMatrix trotinetesMatrix;
    private static TrotinetesReservadasMap trotinetesReservadasMap;
    private static RewardsMap rewardsMap;
    private static UsersMap usersMap;
    private NotificationManagerList notificationManager;

    public ServerManager() {
        trotinetesMatrix = new TrotinetesMatrix();
        trotinetesReservadasMap = new TrotinetesReservadasMap();
        rewardsMap = new RewardsMap();
        usersMap = new UsersMap();
        notificationManager = new NotificationManagerList();
    }

    public void registerUser(String username, String password) throws Exception {
        try {
            usersMap.l.lock();
            if (usersMap.containsKey(username)) throw new Exception("Username já está em uso!");
            usersMap.put(username, new User(username, password));
        } finally {
            usersMap.l.unlock();
        }
    }

    public void loginUser(String username, String password) throws Exception {
        try {
            usersMap.l.lock();
            User u = usersMap.get(username);
            if (u == null) throw new Exception("Username errado!");
            if (!u.getPassword().equals(password)) throw new Exception("Password errada!");
            if (u.isLogged) throw new Exception("User já está ligado!");
            u.login();
        } finally {
            usersMap.l.unlock();
        }
    }

    public List<Point> listFreeTrotinetesOnCoords(Point spot) {

        try {
            trotinetesMatrix.l.lock();
            trotinetesReservadasMap.l.lock();

            List<Point> trotinetes_list = new ArrayList<>();

            for (int x = spot.x - FIXED_DISTANCE; x <= spot.x + FIXED_DISTANCE; x++)
                for (int y = spot.y - FIXED_DISTANCE; y <= spot.y + FIXED_DISTANCE; y++)
                    if (x >= 0 && x < SIZE_MAP && y >= 0 && y < SIZE_MAP
                            && Point.manhattanDistance(new Point(x, y), spot) <= FIXED_DISTANCE)
                        for (ServerManager.Trotinete t : trotinetesMatrix.tm[x][y])
                            if (t.isLivre()) {
                                trotinetes_list.add(new Point(x, y));
                                break;
                            }

            return trotinetes_list;
        } finally {
            trotinetesMatrix.l.unlock();
            trotinetesReservadasMap.l.unlock();
        }
    }

    public static boolean existFreeTrotinetesOnCoords(Point spot) {

        try {
            trotinetesMatrix.l.lock();
            trotinetesReservadasMap.l.lock();

            for (int x = spot.x - FIXED_DISTANCE; x <= spot.x + FIXED_DISTANCE; x++)
                for (int y = spot.y - FIXED_DISTANCE; y <= spot.y + FIXED_DISTANCE; y++)
                    if (x >= 0 && x < SIZE_MAP && y >= 0 && y < SIZE_MAP
                            && Point.manhattanDistance(new Point(x, y), spot) <= FIXED_DISTANCE)
                        for (ServerManager.Trotinete t : trotinetesMatrix.tm[x][y])
                            if (t.isLivre())
                                return true;

            return false;
        } finally {
            trotinetesMatrix.l.unlock();
            trotinetesReservadasMap.l.unlock();
        }
    }

    public static List<Reward> listRewardsOnCoords(Point spot) {

        try {
            rewardsMap.l.lock();

            List<Reward> reward_list = new ArrayList<>();

            for (int x = spot.x - FIXED_DISTANCE; x <= spot.x + FIXED_DISTANCE; x++)
                for (int y = spot.y - FIXED_DISTANCE; y <= spot.y + FIXED_DISTANCE; y++)
                    if (x >= 0 && x < SIZE_MAP && y >= 0 && y < SIZE_MAP
                            && Point.manhattanDistance(new Point(x, y), spot) <= FIXED_DISTANCE)
                        if (rewardsMap.containsKey(new Point(x, y)))
                            reward_list.add(rewardsMap.get(new Point(x, y)));

            return reward_list;

        } finally {
            rewardsMap.l.unlock();
        }
    }

    public static boolean existRewardsOnCoords(Point spot) {

        try {
            rewardsMap.l.lock();

            for (int x = spot.x - FIXED_DISTANCE; x <= spot.x + FIXED_DISTANCE; x++)
                for (int y = spot.y - FIXED_DISTANCE; y <= spot.y + FIXED_DISTANCE; y++)
                    if (x >= 0 && x < SIZE_MAP && y >= 0 && y < SIZE_MAP
                            && Point.manhattanDistance(new Point(x, y), spot) <= FIXED_DISTANCE)
                        if (rewardsMap.containsKey(new Point(x, y)))
                            return true;

            return false;
        } finally {
            rewardsMap.l.unlock();
        }
    }

    public String reservaTrotinete(Point spot, String username) {

        Trotinete closest_t = null;
        Point closest_p = null;
        int minDistance = Integer.MAX_VALUE;

        try {

            trotinetesMatrix.l.lock();
            trotinetesReservadasMap.l.lock();

            for (int x = spot.x - FIXED_DISTANCE; x <= spot.x + FIXED_DISTANCE; x++)
                for (int y = spot.y - FIXED_DISTANCE; y <= spot.y + FIXED_DISTANCE; y++)
                    if (x >= 0 && x < SIZE_MAP && y >= 0 && y < SIZE_MAP
                            && Point.manhattanDistance(new Point(x, y), spot) <= FIXED_DISTANCE)
                        for (Trotinete t : trotinetesMatrix.tm[x][y])
                            if (t.isLivre()) {
                                int distance = Point.manhattanDistance(spot, new Point(x, y));
                                if (distance < minDistance) {
                                    minDistance = distance;
                                    closest_t = t;
                                    closest_p = new Point(x, y);
                                }
                                break;
                            }

            if (closest_t == null) return "Não existem Trotinetes nas redondezas desta área";
            closest_t.reservaTrotinete();

            int codeReserva;

            codeReserva = trotinetesReservadasMap.codeReserva;
            trotinetesReservadasMap.codeReserva++;
            trotinetesReservadasMap.put(codeReserva, closest_t);

            try {
                usersMap.l.lock();
                usersMap.get(username).addCodigo(codeReserva);

            } finally {
                usersMap.l.unlock();
            }

            RewardManager.signalRewardManager();

            for (NotificationManager nm : notificationManager) nm.signalNotification();

            return closest_p + " Codigo: " + codeReserva;
        } finally {
            trotinetesMatrix.l.unlock();
            trotinetesReservadasMap.l.unlock();
        }


    }

    public double estacionaTrotinete(Point destino, String username, int codeReserva) throws Exception {

        try {
            trotinetesReservadasMap.l.lock();
            trotinetesMatrix.l.lock();

            if (!trotinetesReservadasMap.containsKey(codeReserva))
                throw new Exception("Código não reservado");
            if (destino.x < 0 || destino.x > SIZE_MAP || destino.y < 0 || destino.y > SIZE_MAP)
                throw new Exception("Zona fora do mapa");
            if (!usersMap.get(username).containsCodigo(codeReserva))
                throw new Exception("User não reservou esta Trotinete");

            Trotinete t = trotinetesReservadasMap.get(codeReserva);

            double price = 0;

            try {
                rewardsMap.l.lock();

                if (rewardsMap.containsKey(t.coordenadas)) {
                    Reward r = rewardsMap.get(t.coordenadas);
                    if (r.getOrigem().equals(t.coordenadas) && r.getDestino().equals(destino))
                        price = t.recompensaViagem(destino);
                    rewardsMap.remove(t.coordenadas);
                } else
                    price = t.custoViagem(destino);
            } finally {
                rewardsMap.l.unlock();
            }

            trotinetesReservadasMap.remove(codeReserva);
            trotinetesMatrix.tm[t.coordenadas.x][t.coordenadas.y].remove(t);

            trotinetesMatrix.tm[destino.x][destino.y].add(t);
            t.estacionaTrotinete(destino);

            try {
                usersMap.l.lock();
                usersMap.get(username).removeCodigo(codeReserva);
            } finally {
                usersMap.l.unlock();
            }

            RewardManager.signalRewardManager();

            for (NotificationManager nm : notificationManager) nm.signalNotification();

            return price;

        } finally {
            trotinetesReservadasMap.l.unlock();
            trotinetesMatrix.l.unlock();
        }

    }

    public void exit(String username, NotificationManager nm) {
        try {
            usersMap.l.lock();
            trotinetesReservadasMap.l.lock();
            trotinetesMatrix.l.lock();
            User u = usersMap.get(username);
            for (int i : u.getCodigosReservados()) {
                trotinetesReservadasMap.get(i).livre = true;
                trotinetesReservadasMap.remove(i);
            }
            u.deleteCodigosReservados();
            u.logoff();
            unsubscribe(nm);
        } finally {
            usersMap.l.unlock();
            trotinetesReservadasMap.l.unlock();
            trotinetesMatrix.l.unlock();
        }

    }

    public ReentrantLock getRewardsMapLock() {
        return rewardsMap.l;
    }

    public static void updateRewards() {

        try {
            trotinetesMatrix.l.lock();
            trotinetesReservadasMap.l.lock();
            rewardsMap.l.lock();

            rewardsMap
                    .entrySet()
                    .removeIf((entry -> existRewardsOnCoords(entry.getValue().getDestino())));

            for (int i = 0; i < SIZE_MAP; i++)
                for (int j = 0; j < SIZE_MAP; j++) {
                    Point origem = new Point(i, j);
                    if (rewardsMap.containsKey(origem)) break;
                    int freeTrotinetesOrigem = (int) trotinetesMatrix.tm[origem.x][origem.y]
                            .stream()
                            .filter(Trotinete::isLivre)
                            .count();
                    if (freeTrotinetesOrigem > 1) {
                        //System.out.println(origem);
                        for (int x = 0; x < SIZE_MAP; x++) {
                            boolean outerloop = true;
                            for (int y = 0; y < SIZE_MAP; y++) {
                                Point destino = new Point(x, y);
                                if (!existFreeTrotinetesOnCoords(destino)) {
                                    rewardsMap.put(origem, new Reward(origem, destino));
                                    outerloop = false;
                                    break;
                                }
                            }
                            if (!outerloop) break;
                        }
                    }
                }
            //System.out.println(rewardsMap.values().stream().collect(Collectors.toList()));
        } finally {
            trotinetesMatrix.l.unlock();
            trotinetesReservadasMap.l.unlock();
            rewardsMap.l.unlock();
        }
    }

    public void subscribe(NotificationManager nm){
        try{
            notificationManager.l.lock();
            notificationManager.add(nm);
        }
        finally {
            notificationManager.l.unlock();
        }

    }

    public void unsubscribe(NotificationManager nm){
        try {
            notificationManager.l.lock();
            notificationManager.remove(nm);
        }
        finally {
            notificationManager.l.unlock();
        }
    }


}