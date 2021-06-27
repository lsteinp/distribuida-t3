import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class FileReader {

    private ArrayList<Process> listOfProcess = new ArrayList<>();

    public void read() {
        try {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("config.txt");
            Scanner reader = new Scanner(in);
            while (reader.hasNextLine()) {
                String[] stringArray = reader.nextLine().split(" ");
                Process process = new Process(
                        Integer.parseInt(stringArray[0]),
                        stringArray[1],
                        Integer.parseInt(stringArray[2]),
                        Double.parseDouble(stringArray[3]),
                        Integer.parseInt(stringArray[4]),
                        Integer.parseInt(stringArray[5]),
                        Integer.parseInt(stringArray[6])
                );
                listOfProcess.add(process);
                System.out.println(Arrays.toString(stringArray));
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Process getProcess(int id){
        return listOfProcess.get(id);
    }
    public int processQuantity() {
        return listOfProcess.size();
    }
}
