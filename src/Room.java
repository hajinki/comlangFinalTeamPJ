import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Room {
    private int rows;
    private int cols;
    private char[][] grid;

    public Room(String filename) throws IOException {
        loadFromCSV(filename);
    }

    private void loadFromCSV(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String[] size = reader.readLine().split(",");
        rows = Integer.parseInt(size[0].trim());
        cols = Integer.parseInt(size[1].trim());
    
        // 임시로 줄 저장
        String[] lines = new String[rows];
        for (int i = 0; i < rows; i++) {
            lines[i] = reader.readLine();
            // 실제 cols보다 길다면 그에 맞춰 확장
            int actualCols = lines[i].split(",", -1).length;
            if (actualCols > cols) cols = actualCols;
        }
    
        // 배열 재할당
        grid = new char[rows][cols];
    
        for (int i = 0; i < rows; i++) {
            String[] tokens = lines[i].split(",", -1);
            for (int j = 0; j < cols; j++) {
                if (j < tokens.length && !tokens[j].isBlank()) {
                    grid[i][j] = tokens[j].trim().charAt(0);
                } else {
                    grid[i][j] = ' ';
                }
            }
        }
    
        reader.close();
    }
    

    public void printRoom() {
        System.out.println("+".repeat(cols + 2));
        for (int i = 0; i < rows; i++) {
            System.out.print("|");
            for (int j = 0; j < cols; j++) {
                System.out.print(grid[i][j]);
            }
            System.out.println("|");
        }
        System.out.println("+".repeat(cols + 2));
    }

    public char[][] getGrid() {
        return grid;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
}
