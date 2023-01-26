public class Reward {

    private final Point origem, destino;

    public Reward(Point origem, Point destino) {
        this.origem = origem.clone();
        this.destino = destino.clone();
    }

    @Override
    public String toString() {
        return "Origem: " + origem +  " Destino: " + destino;
    }

    public Point getOrigem() {
        return origem.clone();
    }

    public Point getDestino() {
        return destino.clone();
    }

}