import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

public class Main {

    private static int processId;
    private static InetAddress group;
    private static int port;
    private static boolean isServer = false;
    private static MulticastSocket socket;
    private static int internalClock = 0;
    private static final ArrayList<Integer> vetorialClock = new ArrayList<>();
    private static boolean start = false;
    public static int processQuantity;
    public static Process process;
    public static HashMap<Integer, Process> connectedProcess = new HashMap<>();
    public static ArrayList<String> actualMessage = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length < 1) {
            System.out.println("Invalid arguments");
            System.exit(1);
        }

        processId = Integer.parseInt(args[0]);
        port = 8888;
        socket = new MulticastSocket(port);
        group = InetAddress.getByName("224.0.0.0");
        socket.joinGroup(group);
        isServer = args[1].equals("server");

        FileReader fileReader = new FileReader();
        fileReader.read();
        process = fileReader.getProcess(processId);
        processQuantity = fileReader.processQuantity();
        for (int i = 0; i < processQuantity; i++ ) {
            vetorialClock.add(0);
        }

        listening();

        while (true) {
            if (isServer) {
                if (!start) {
                    if (connectedProcess.size() == processQuantity) {
                        Thread.sleep(1000);
                        serverStart(socket);
                        socket.leaveGroup(group);
                        socket.close();
                        System.exit(1);
                    } else {
                        System.out.println("Waiting for nodes");
                    }
                }
            } else {
                if (!start) {
                    clientPing(socket);
                } else {
                    Random r = new Random();
                    double randomNumber = r.nextDouble();
                    double chance = process.chance;

                    if (randomNumber < chance) {
                        send();
                    } else {
                        local();
                    }
                }
                if (internalClock >= process.events) {
                    System.out.println("Processo encerrado por terminar suas acoes");
                    socket.leaveGroup(group);
                    socket.close();
                    System.exit(1);
                }
            }
            long sleepTime = generateSleep(process.min_delay, process.max_delay);
            Thread.sleep(sleepTime);
        }
    }

    private static long generateSleep(int min_delay, int max_delay) {
        Random r2 = new Random();
        return r2.nextInt(max_delay-min_delay+1) + min_delay;
    }

    private static void send() {
        incrementInternalClock();

        int randomProcess = processId;
        while (randomProcess == processId) {
            Random r = new Random();
            randomProcess = r.nextInt(processQuantity);
        }
        System.out.println(processId + " " + vetorialClock + " S " + randomProcess);

        String message = "s " + processId + " " + (randomProcess) + " " + formatVetorialClock();
        try {
            byte[] start;
            start = message.getBytes();
            DatagramPacket udp = new DatagramPacket(start, start.length, group, port);
            socket.send(udp);
        } catch (Exception e) {}
        waitResponse(randomProcess);
    }

    private static void waitResponse(int receiver) {
        Thread thread1 = new Thread(() -> {
            int counter = 0;
            while (counter < 3) {
                try {
                    Thread.sleep(1000);
                    counter++;
                } catch (InterruptedException e) {}
            }
            System.out.println("Processo encerrado pela ausencia de resposta de " + receiver + " tempo: "
                    + vetorialClock);
            System.exit(1);
        });

        Thread thread2 = new Thread(() -> {
            boolean encerrado = false;
            while (!encerrado) {
                try {
                    for (String data : actualMessage) {
                        if (data.split(" ")[0].equals("r")) {

                            if (data.split(" ")[2].equals(processId + "")) {
                                if (data.split(" ")[1].equals(receiver + "")) {
                                    actualMessage.remove(data);
                                    thread1.stop();
                                    encerrado = true;
                                    break;
                                }
                            }
                        }
                    }

                } catch (Exception e) {}
            }
        });
        thread1.start();
        thread2.start();
    }

    private static void local() {
        incrementInternalClock();
        System.out.println(processId + " " + vetorialClock);
    }

    private static void incrementInternalClock() {
        internalClock++;
        vetorialClock.set(processId, internalClock);
    }

    private static String formatVetorialClock(){
        String formated = "";
        for (int i= 0; i< processQuantity; i++){
            formated += vetorialClock.get(i) + ",";
        }
        return formated.substring(0, formated.length() - 1);
    }

    private static void listening() {
        Thread thread1 = new Thread(() -> {
            while (true) {
                byte[] response = new byte[2048];
                DatagramPacket res = new DatagramPacket(response, response.length);
                try {
                    socket.receive(res);
                    String data = new String(res.getData(), 0, res.getLength());
                    if (isServer) {
                        if (data.contains("ping:")) {
                            int newProcess = Integer.parseInt(data.split(":")[1]);
                            connectedProcess.put(newProcess - 1, process);
                        }
                    } else {
                        if (!start) {
                            if (data.equals("start")) {
                                System.out.println(processId + " Start " + vetorialClock);
                                start = true;
                            }
                        }
                        else {
                            if (data.split(" ")[0].equals("s")) {
                                if (data.split(" ")[2].equals(processId + "")) {
                                    ArrayList<String> recievedVetorialClock = new ArrayList<>(Arrays.asList(data.split(" ")[3].split(",")));
                                    incrementInternalClock();
                                    for(int i = 0 ; i < processQuantity; i++){
                                        vetorialClock.set(i, Math.max(vetorialClock.get(i),
                                                Integer.parseInt(recievedVetorialClock.get(i))));
                                    }
                                    System.out.println(processId + " " + vetorialClock
                                            + " R " + data.split(" ")[1] + " " + recievedVetorialClock);

                                    String message = "r " + processId + " " + data.split(" ")[1];
                                    try {
                                        byte[] start;
                                        start = message.getBytes();
                                        DatagramPacket udp = new DatagramPacket(start, start.length, group, port);
                                        socket.send(udp);
                                    } catch (Exception e) {}
                                }
                            } else if (data.split(" ")[0].equals("r")) {

                                int origin = Integer.parseInt(data.split(" ")[2]);

                                if (origin == processId) {
                                    actualMessage.add(data);
                                }
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        });
        thread1.start();
    }

    public static void clientPing(MulticastSocket socket) {
        String message = "ping:" + processId;
        try {
            byte[] start;
            start = message.getBytes();
            DatagramPacket udp = new DatagramPacket(start, start.length, group, port);
            socket.send(udp);
        } catch (Exception e) {}
    }

    public static void serverStart(MulticastSocket socket) {
        startServer();
        String message = "start";
        try {
            byte[] start;
            start = message.getBytes();
            DatagramPacket udp = new DatagramPacket(start, start.length, group, port);
            socket.send(udp);
            System.out.println("Start");
        } catch (Exception e) {}
        start = true;
    }

    public static void startServer() {
        System.out.println("Starting nodes");
    }
}