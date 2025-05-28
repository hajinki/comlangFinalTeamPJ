import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Room {
    private int rows;
    private int cols;
    private char[][] grid;
    private Monster[][] monsters;
    private String path; // 🔸 이게 있어야 FileManager.loadGrid(path) 가능함

    public Room(String filename) throws IOException {
        this.path = filename; // 🔸 먼저 path 저장
        loadFromCSV(filename); // 🔸 CSV 파일 읽어서 grid 채움
        monsters = new Monster[rows][cols]; // 🔸 몬스터 배열은 grid랑 같은 크기
    }

    public Monster getMonsterAt(int x, int y) {
        return monsters[y][x];
    }

    public void setMonsterAt(int x, int y, Monster m) {
        monsters[y][x] = m;
    }

    private void loadFromCSV(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String[] size = reader.readLine().split(",");
        
        int baseRows = Integer.parseInt(size[0].trim());
        int baseCols = Integer.parseInt(size[1].trim());
    
        // room3이면 1씩 확장
        boolean isRoom3 = filename.contains("room3.csv");
        rows = isRoom3 ? baseRows + 1 : baseRows;
        cols = isRoom3 ? baseCols + 1 : baseCols;
    
        // 파일에서 읽을 줄 수는 baseRows 개
        String[] lines = new String[baseRows];
        for (int i = 0; i < baseRows; i++) {
            lines[i] = reader.readLine();
            int actualCols = lines[i].split(",", -1).length;
            if (actualCols > cols) cols = actualCols;
        }
    
        grid = new char[rows][cols];
    
        for (int i = 0; i < rows; i++) {
            if (i < baseRows) {
                String[] tokens = lines[i].split(",", -1);
                for (int j = 0; j < cols; j++) {
                    if (j < tokens.length && !tokens[j].isBlank()) {
                        grid[i][j] = tokens[j].trim().charAt(0);
                    } else {
                        grid[i][j] = ' ';
                    }
                }
            } else {
                // 확장된 마지막 행은 공백 채우기
                for (int j = 0; j < cols; j++) {
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

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public String getPath() {
        return path;
    }
}
