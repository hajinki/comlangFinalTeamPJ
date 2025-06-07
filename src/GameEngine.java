import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.awt.Point;
import java.util.Map;
import java.util.HashMap;

public class GameEngine {
    private Room room;
    private Hero hero;
    private boolean isNewGame = true; // true, when new game start


    public void start() {
        System.out.println("=== Solo Adventure Maze ===");
        try {
            isNewGame = true; 
            initializeDoorLinks();
            room = new Room("data/room1.csv");
            placeHero(); // find @ location or  (1,1) or random
            gameLoop();
        } catch (IOException e) {
            System.out.println("fail to load room: " + e.getMessage());
        }
    }

    private void placeHero() {
        if (!isNewGame) return;

        char[][] grid = room.getGrid();
        int rows = room.getRows();
        int cols = room.getCols();
    
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == '@') {
                    if (hero == null) {
                        hero = new Hero(j, i); // firs execution
                    } else {
                        hero.setPosition(j, i); // reuse original hero
                    }
                    return;
                }
            }
        }
    
        // fallback when cannot find hero → no make new hero
        if (grid[1][1] == ' ') {
            if (hero == null) {
                hero = new Hero(1, 1);
            } else {
                hero.setPosition(1, 1);
            }
        } else {
            outer:
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (grid[i][j] == ' ') {
                        if (hero == null) {
                            hero = new Hero(j, i);
                        } else {
                            hero.setPosition(j, i);
                        }
                        break outer;
                    }
                }
            }
        }
    }
    
    

    private void gameLoop() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
    
        while (true) {
            printStatus();
            updateGrid();
            room.printRoom();
    
            checkForCombat();
    
            System.out.print("click to move (u/d/l/r): ");
            input = reader.readLine();
            if (input == null || input.length() == 0) continue;
    
            char cmd = input.charAt(0);
            int newX = hero.getX();
            int newY = hero.getY();
    
            switch (cmd) {
                case 'u' -> newY--;
                case 'd' -> newY++;
                case 'l' -> newX--;
                case 'r' -> newX++;
                default -> {
                    System.out.println("wrong instruction.");
                    continue;
                }
            }
    
            if (!canMoveTo(newX, newY)) {
                System.out.println("can't move");
                continue;
            }
    
            char[][] grid = room.getGrid();
            char tile = grid[newY][newX];
            if (tile == 'D' && !hero.hasKey()) {
                System.out.println("You need key to open master door");
                continue;  // block to move
            }

    
            // if door do tryDoor() 
            if (tile == 'D' || tile == 'd') {
                if (tryDoor(newX, newY)) continue;
            }
    
            // if monster, block
            if (tile == 'G' || tile == 'T' || tile == 'o') {
                System.out.println("A monster is blocking the road ! Do you want attack?");
                continue;
            }
    
            // 무기 발견
            Weapon found = switch (tile) {
                case 'S' -> new Weapon("Stick", 1);
                case 'W' -> new Weapon("Weak Sword", 2);
                case 'X' -> new Weapon("Strong Sword", 3);
                default -> null;
            };
    
            if (found != null) {
                if (hero.getWeapon() == null) {
                    hero.setWeapon(found);
                    System.out.println("get Weapon : " + found.getName());
                    grid[newY][newX] = ' ';
                } else {
                    System.out.println("find" + found.getName() + "' ! now weapon: " + hero.getWeapon().getName());
                    System.out.print("Do you want to change this weapon? (y/n): ");
                    input = reader.readLine();
                    if (input.equalsIgnoreCase("y")) {
                        hero.setWeapon(found);
                        grid[newY][newX] = ' ';
                        System.out.println("Change weapon to" + found.getName() + " !");
                    } else {
                        System.out.println("cancle change weapon");
                        // when cancle weapon change, location is continue
                        char weaponSymbol = switch (found.getName()) {
                            case "Stick" -> 'S';
                            case "Weak Sword" -> 'W';
                            case "Strong Sword" -> 'X';
                            default -> ' ';
                        };
                        grid[newY][newX] = weaponSymbol;
                        continue; // not move
                    }
                }
            }
    
            if (!checkForPotion(newX, newY)) {
                continue;  // when do not drink potion, not move
            }
    
            // move
            grid[hero.getY()][hero.getX()] = ' ';  // remove now location
            hero.setPosition(newX, newY);          // reload location
            grid[newY][newX] = '@';                // new location'@'
        }
    }
    
            
     

    private void checkForCombat() throws IOException {
        int x = hero.getX();
        int y = hero.getY();
        char[][] grid = room.getGrid();
    
        int[][] directions = { {0,-1}, {0,1}, {-1,0}, {1,0}, {-1,-1}, {-1,1},{1,-1},{1,1} };
    
        for (int[] d : directions) {
            int nx = x + d[0];
            int ny = y + d[1];
    
            if (nx >= 0 && ny >= 0 && ny < room.getRows() && nx < room.getCols()) {
                char c = grid[ny][nx];
    
                if (c == 'G' || c == 'O' || c == 'T') {
                    // if have original monster
                    Monster monster = room.getMonsterAt(nx, ny);
    
                    // if not make new room
                    if (monster == null) {
                        monster = new Monster(c);
                        room.setMonsterAt(nx, ny, monster);
                    }
    
                    // combat start
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

                    while (true) {
                        System.out.println("find monster! type: " + monster.getType() + " | HP: " + monster.getHp());
                        System.out.print("You want attack? (y/n): ");
                        String input = reader.readLine();

                        if (input.equalsIgnoreCase("y")) {
                            if (hero.getWeapon() == null) {
                                System.out.println("You have to get weapon");
                                break;
                            }

                            monster.takeDamage(hero.getWeapon().getDamage());
                            hero.changeHp(-monster.getDamage());

                            System.out.println("You've been damaged " + monster.getDamage() + " point");
                            System.out.println("You attack to " + monster.getType() + " " + hero.getWeapon().getDamage() + " point");
                            System.out.println("❤️ current HP: " + hero.getHp());


                            
                            if (hero.getHp() <= 0) {
                                System.out.println("you died. game over!");
                                System.exit(0);
                            }

                            if (monster.isDead()) {
                                System.out.println("success to kill monster!");
                                if (c == 'T') {
                                    hero.obtainKey();
                                    System.out.println("get key!");
                                }
                                grid[ny][nx] = ' ';
                                room.setMonsterAt(nx, ny, null);
                                updateGrid();
                                room.printRoom();
                                break; // stop conbat
                            } else {
                                // HP , only monster live
                                System.out.println("🩸 monster HP: " + monster.getHp());
                                

                                if (hero.getHp() <= 5) {
                                    System.out.println("very low stamina! Use potion or avoid combat");
                                }
                                room.printRoom();
                            }

                        } else if (input.equalsIgnoreCase("n")) {
                            System.out.println("Avoiced Combat");
                            break;
                        } else {
                            System.out.println("Invalid input. Please enter y or n.");
                        }
                    }

                }
            }
        }
    }
    
private Map<String, Map<Point, DoorLink>> doorMap = new HashMap<>();

private static class DoorLink {
    String filename;
    Point position;

    public DoorLink(String filename, Point position) {
        this.filename = filename;
        this.position = position;
    }
}

private void initializeDoorLinks() {
    // room1 (0,0) <-> room2 (2,5)
    addDoorLink("room1.csv", new Point(0, 0), "room2.csv", new Point(2, 5));
    addDoorLink("room2.csv", new Point(2, 5), "room1.csv", new Point(0, 0));

    // room2 (5,0) <-> room3 (0,0)
    addDoorLink("room2.csv", new Point(5, 0), "room3.csv", new Point(4, 4));
    addDoorLink("room3.csv", new Point(4, 4), "room2.csv", new Point(5, 0));


    addDoorLink("room4.csv", new Point(0, 5), "room3.csv", new Point(0, 0));
    addDoorLink("room3.csv", new Point(0, 0), "room4.csv", new Point(0, 5));

    addDoorLink("room4.csv", new Point(4, 0), "room3.csv", new Point(0, 0));  // master door D
    addDoorLink("room3.csv", new Point(0, 0), "room4.csv", new Point(4, 0));


}


private void addDoorLink(String fromRoom, Point fromPos, String toRoom, Point toPos) {
    doorMap.computeIfAbsent(fromRoom, k -> new HashMap<>())
           .put(fromPos, new DoorLink(toRoom, toPos));
}


    
    private boolean tryDoor(int x, int y) throws IOException {
        char tile = room.getGrid()[y][x];
        // String targetFilename = room.getDoorFilenameAt(x, y);
        String currentRoomName = new File(room.getPath()).getName(); // ex: room1.csv
        DoorLink link = doorMap.getOrDefault(currentRoomName, new HashMap<>()).get(new Point(x, y));
        
        if (link == null) {
            System.out.println("no connected file to door");
            return false;
        }
        // ✅ Master door (D): 열쇠 필요
        if (tile == 'D' && !hero.hasKey()) {
            System.out.println("Master door. need key");
            return false;
        }
        
        if (room.getPath().contains("room4.csv") && x == 0 && y == 5) {
            System.out.println("Congratulations! I opened the master door and cleared the game!");
            System.exit(0); // 게임 종료
            return true;
        }

        
        
        String targetFilename = link.filename;
        Point newHeroPos = link.position;
    

        String currentPath = room.getPath();  // now room route (예: data/room1.csv)

        
    
        System.out.println("Open the door and move to the next room!");
    

        // save now room status
        String currentSavePath = room.getPath().replace("data/", "save/");
        FileManager.saveRoom(currentSavePath, room.getGrid());

        // ready next room info
        String nextRoomName = link.filename;
        String dataPath = "data/" + nextRoomName;
        String savePath = "save/" + nextRoomName;
        
        File saveFile = new File(savePath);
        
        // if not room in save → bring data , because first visit
    if (!saveFile.exists()) {
        Room tempRoom = new Room(dataPath);           // data에서 최초 로드
        FileManager.saveRoom(savePath, tempRoom.getGrid()); // save에 저장
    }

    // reload in save
    room = new Room(savePath);
    isNewGame = true;  
    placeHero();
    updateGrid();

    return true;
    }
    

    private boolean hasLivingMonsters() {
        for (int i = 0; i < room.getRows(); i++) {
            for (int j = 0; j < room.getCols(); j++) {
                char tile = room.getGrid()[i][j];
                if (tile == 'G' || tile == 'O' || tile == 'T') {
                    return true;
                }
    
                Monster m = room.getMonsterAt(j, i);
                if (m != null && !m.isDead()) {
                    return true;
                }
            }
        }
        return false;
    }

    
    

    
    
    
    
    
    

    private boolean checkForPotion(int x, int y) {
        char[][] grid = room.getGrid();
        char nextTile = grid[y][x];
    
        if (nextTile == 'm' || nextTile == 'B') {
            int recover = (nextTile == 'm') ? 6 : 12;
    
            if (hero.getHp() < Hero.MAX_HP) {
                int before = hero.getHp();
                hero.changeHp(recover);
                System.out.println(" Drink potion! HP: " + before + " → " + hero.getHp());
                grid[y][x] = ' ';  // 포션 제거
            } else {
                System.out.println("Find potion, but potion is full.");
                return false;  // 이동하지 않음
            }
        }
        return true;  // 이동 가능
    }
    

    
    
    

    private void updateGrid() {
        char[][] grid = room.getGrid();
    
        // remove original @ 
        for (int i = 0; i < room.getRows(); i++) {
            for (int j = 0; j < room.getCols(); j++) {
                if (grid[i][j] == '@') grid[i][j] = ' ';
            }
        }
    
        int y = hero.getY();
        int x = hero.getX();
    
        // '@' when not monster is in upper
        char cell = grid[y][x];
        if (cell != 'G' && cell != 'O' && cell != 'T') {
            grid[y][x] = '@';
        }
    }
    

    private boolean canMoveTo(int x, int y) {
        if (x < 0 || y < 0 || y >= room.getRows() || x >= room.getCols()) return false;
        char cell = room.getGrid()[y][x];
        return cell != 'G' && cell != 'O' && cell != 'T';  // monster not pass
    }

    private void printStatus() {
        System.out.println("HP: " + hero.getHp() + 
            " | Weapon: " + (hero.getWeapon() != null ? hero.getWeapon().getName() : "no") + 
            " | key: " + (hero.hasKey() ? "no" : "yes"));
    }
}
