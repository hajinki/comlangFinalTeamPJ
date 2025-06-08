import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.awt.Point;

public class Room {
    private int rows;
    private int cols;
    private char[][] grid;
    private Monster[][] monsters;
    private String path;
    private Map<String, Point> doorMap = new HashMap<>(); // door label → position
    private Map<Point, String> reverseDoorMap = new HashMap<>(); // position → filename

    public Room(String filename) throws IOException {
        this.path = filename;
        loadFromCSV(filename);
        monsters = new Monster[rows][cols];
    }

    public Monster getMonsterAt(int x, int y) {
        return monsters[y][x];
    }

    public void setMonsterAt(int x, int y, Monster m) {
        monsters[y][x] = m;
    }

    public String getDoorFilenameAt(int x, int y) {
        return reverseDoorMap.get(new Point(x, y));
    }

    private void loadFromCSV(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String[] size = reader.readLine().split(",");

        int baseRows = Integer.parseInt(size[0].trim());
        int baseCols = Integer.parseInt(size[1].trim());

        boolean isRoom3 = filename.contains("room3.csv");
        rows = isRoom3 ? baseRows + 1 : baseRows;
        cols = isRoom3 ? baseCols + 1 : baseCols;//+1 when room 3 because of problem...

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
                        String token = tokens[j].trim();
                        char tile = token.charAt(0);

                        // Parse doors of form d:room2.csv
                        if ((tile == 'd' || tile == 'D') && token.contains(":")) {
                            String[] parts = token.split(":");
                            if (parts.length == 2) {
                                reverseDoorMap.put(new Point(j, i), parts[1]);

                                doorMap.put(parts[1], new Point(j, i));
                        
                                //hard coding error but we can't fixed it...
                                if (parts[1].contains("room4.csv")) {
                                    if (filename.contains("room3.csv") && i == 0 && j == 0) {
                                        grid[i][j] = 'd';  // room3 0.0 d
                                    } else {
                                        grid[i][j] = 'D';  // 
                                    }
                                } else {
                                    grid[i][j] = 'd'; // Regular door
                                }
                            } else {
                                if (tile == '@') {
                                    grid[i][j] = ' '; // Processing if it is not a regular tile (m, G, W, etc.) or a door.
                                    //@ is a temporary symbol representing coordinates immediately after movement, so save it as blank unconditionally
                                } else {
                                    grid[i][j] = tile;
                                }
                            }
                        } else {
                            if (tile == '@') {
                                grid[i][j] = ' '; // blank if cell blank
                            } else {
                                grid[i][j] = tile;
                            } 
                        } 

                    } else {
                        grid[i][j] = ' ';//room3 blank all ' '
                    }
                }
            } else {
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

    public Map<Point, String> getReverseDoorMap() {
        return reverseDoorMap;
    }

    public String getFilename() {
        return path;
    }

    public void saveToFile(String filename) {
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(filename))) {
            writer.write(rows + "," + cols);
            writer.newLine();
            for (int i = 0; i < rows; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < cols; j++) {
                    sb.append(grid[i][j]);
                    if (j < cols - 1) sb.append(",");
                }
                writer.write(sb.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("fail to room save: " + e.getMessage());
        }
    }

} 


