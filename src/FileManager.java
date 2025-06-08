import java.io.*;
import java.util.List;

public class FileManager {
    public static void clearSaveFolder() {
        File saveFolder = new File("save");

        if (saveFolder.exists() && saveFolder.isDirectory()) {
            File[] files = saveFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete(); 
                    }
                }
            }
        }
    }
    public static char[][] loadRoom(String filepath, int[] size) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filepath));
        String[] tokens = reader.readLine().split(",");
        int rows = Integer.parseInt(tokens[0].trim());
        int cols = Integer.parseInt(tokens[1].trim());

        size[0] = rows;
        size[1] = cols;

        char[][] grid = new char[rows][cols];
        for (int i = 0; i < rows; i++) {
            String line = reader.readLine();
            String[] chars = line.split(",", -1);
            for (int j = 0; j < cols; j++) {
                grid[i][j] = (j < line.length()) ? line.charAt(j) : ' ';
            }
        }

        reader.close();
        return grid;
    }

    public static void saveRoom(String filepath, char[][] grid) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));

        int rows = grid.length;
        int cols = grid[0].length;
        writer.write(rows + "," + cols);
        writer.newLine();

        for (int i = 0; i < rows; i++) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < cols; j++) {
                char ch = grid[i][j];
                if (ch == '@') ch = ' ';
                line.append(ch);
                if (j < cols - 1) line.append(",");
            }
            writer.write(line.toString());
            writer.newLine();
        }

        writer.close();
    }
}
