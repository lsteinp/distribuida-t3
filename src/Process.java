public class Process {
    public int id;
    public String host;
    public int port;
    public double chance;
    public int events;
    public int min_delay;
    public int max_delay;

    public Process(int id, String host, int port, double chance, int events, int min_delay, int max_delay) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.chance = chance;
        this.events = events;
        this.min_delay = min_delay;
        this.max_delay = max_delay;
    }

}
