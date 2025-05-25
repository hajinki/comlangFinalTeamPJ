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
        rows = Integer.parseInt(size[0].trim());
        cols = Integer.parseInt(size[1].trim());

        // 줄 전체 저장 후 파싱
        String[] lines = new String[rows];
        for (int i = 0; i < rows; i++) {
            lines[i] = reader.readLine();
            int actualCols = lines[i].split(",", -1).length;
            if (actualCols > cols) cols = actualCols;
        }

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
