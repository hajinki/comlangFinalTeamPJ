import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.awt.Point;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    private Room room;
    private Hero hero;
    private boolean isNewGame = true; // true, when new game start

    
    public void start() {
        FileManager.clearSaveFolder();
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
        
        boolean found = false;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == '@') {
                    if (hero == null) {//null mean= hero first made
                        hero = new Hero(j, i); // firs execution
                    } else {
                        hero.setPosition(j, i); // reuse original hero
                    }
                    found = true;
                    break;
                }
            }
            if (found) break;
        }
        if (found) return;
        

        if (grid[0][0] == ' ') {
            if (hero == null) {
                hero = new Hero(0, 0);
            } else {
                hero.setPosition(0, 0);
            }
            return;
        }
        List<Point> emptySpaces = new ArrayList<>();//Point is a class for storing x,y provided by Java

                                                     //The empty Spaces are a list to store the blank coordinates in the room
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == ' ') {
                    emptySpaces.add(new Point(j, i));//run whole room
                }
            }
        }
    
        if (!emptySpaces.isEmpty()) {//room empty?
            Random rand = new Random();//java random number maker
            Point randomSpot = emptySpaces.get(rand.nextInt(emptySpaces.size()));//choose random index in our list length
            if (hero == null) {
                hero = new Hero(randomSpot.x, randomSpot.y);
            } else {
                hero.setPosition(randomSpot.x, randomSpot.y);
            }
        }
    }
        
    
    
    

    private void gameLoop() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
    
        while (true) {//infinite loop
            printStatus();
            updateGrid();
            room.printRoom();
    
            checkForCombat();//Make sure you're near monsters before you move and that combat conditions have been met
    
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
                continue;//can move?
            }
    
            char[][] grid = room.getGrid();
            char tile = grid[newY][newX];//Check the letters in the coordinates you want to move (tile)
            if (tile == 'D' && !hero.hasKey()) {
                System.out.println("You need key to open master door");
                continue;  // if master door block to move
            }

    
            // if door do tryDoor() -> if success move to new room
            if (tile == 'D' || tile == 'd') {
                if (tryDoor(newX, newY)) continue;
            }
    
            // if monster, block
            if (tile == 'G' || tile == 'T' || tile == 'o') {
                System.out.println("A monster is blocking the road ! Do you want attack?");
                continue;
            }
    
            // weapon found
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
        char[][] grid = room.getGrid();//current map info 
    
        int[][] directions = { {0,-1}, {0,1}, {-1,0}, {1,0}, {-1,-1}, {-1,1},{1,-1},{1,1} };//Up, down, left, right + diagonal
    
        for (int[] d : directions) {//check around cordinates
            int nx = x + d[0];
            int ny = y + d[1];
    
            if (nx >= 0 && ny >= 0 && ny < room.getRows() && nx < room.getCols()) {
                char c = grid[ny][nx];
    
                if (c == 'G' || c == 'O' || c == 'T') {
                    // if monster exist, combat
                    Monster monster = room.getMonsterAt(nx, ny);
    
                    // if not make new room, make monster(new object)
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
                            System.out.println("current HP: " + hero.getHp());


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
                                room.setMonsterAt(nx, ny, null);//if monster die, remove
                                updateGrid();
                                room.printRoom();
                                break; // stop conbat
                            } else {
                                // HP , only monster live
                                System.out.println("monster HP: " + monster.getHp());
                                

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
            char tile = room.getGrid()[y][x];//bring info in current room ex) d
           
            String currentRoomName = new File(room.getPath()).getName(); // ex: room1.csv- bring file name 
            DoorLink link = doorMap.getOrDefault(currentRoomName, new HashMap<>()).get(new Point(x, y));//bring the door info map that match with current room name 
            
            if (link == null) {
                System.out.println("no connected file to door");
                return false;
            }
            // Master door (D)
            if (tile == 'D' && !hero.hasKey()) {
                System.out.println("Master door. need key");
                return false;
            }
            
            if (room.getPath().contains("room4.csv") && x == 0 && y == 5) {
                System.out.println("Congratulations! I opened the master door and cleared the game!");
                System.exit(0); // game end
                return true;
            }
    
            
            
            String targetFilename = link.filename;
            Point newHeroPos = link.position;
        
    
            String currentPath = room.getPath();  // now room route (ex: data/room1.csv)
    
            
        
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
            Room tempRoom = new Room(dataPath);           // data at initial
            FileManager.saveRoom(savePath, tempRoom.getGrid()); // save in 'save'
        }
    
        // reload in save
        room = new Room(savePath);
        isNewGame = true;  
        placeHero();// hero replace
        updateGrid();
    
        return true;
        }
    

    private boolean hasLivingMonsters() {
        for (int i = 0; i < room.getRows(); i++) {
            for (int j = 0; j < room.getCols(); j++) {// rounding room
                char tile = room.getGrid()[i][j];
                if (tile == 'G' || tile == 'O' || tile == 'T') {
                    return true;// living monster
                }
    
                Monster m = room.getMonsterAt(j, i);
                if (m != null && !m.isDead()) {
                    return true;// double check real dead even object
                }
            }
        }
        return false;
    }

    
     private boolean checkForPotion(int x, int y) {
        char[][] grid = room.getGrid();
        char nextTile = grid[y][x];//future place of hero
    
        if (nextTile == 'm' || nextTile == 'B') {
            int recover = (nextTile == 'm') ? 6 : 12;//condition ? when true : when lie
    
            if (hero.getHp() < Hero.MAX_HP) {// when max, potion x
                int before = hero.getHp();// save before drink potion and use to 'before'->'after' hp up!
                hero.changeHp(recover);
                System.out.println(" Drink potion! HP: " + before + " → " + hero.getHp());
                grid[y][x] = ' ';  // potion delete in tile
            } else {
                System.out.println("Find potion, but potion is full.");
                return false;  // potion don't move when don't eat
            }
        }
        return true;  // can move
    }
    

    private void updateGrid() {
        char[][] grid = room.getGrid();
    
        // remove original @ 
        for (int i = 0; i < room.getRows(); i++) {
            for (int j = 0; j < room.getCols(); j++) {
                if (grid[i][j] == '@') grid[i][j] = ' ';
            }
        }
    
        int y = hero.getY();//row
        int x = hero.getX();//col, current hero location
    
        // '@' when not monster is in upper, If it overlaps with a monster, handle it separately (battle, prevention, etc.)
        char cell = grid[y][x];
        if (cell != 'G' && cell != 'O' && cell != 'T') {
            grid[y][x] = '@';
        }
    }
    

    private boolean canMoveTo(int x, int y) {
        if (x < 0 || y < 0 || y >= room.getRows() || x >= room.getCols()) return false;
        char cell = room.getGrid()[y][x];
        return cell != 'G' && cell != 'O' && cell != 'T';  // monster tile not pass
    }

    private void printStatus() {
        System.out.println("HP: " + hero.getHp() + 
            " | Weapon: " + (hero.getWeapon() != null ? hero.getWeapon().getName() : "no") + 
            " | key: " + (hero.hasKey() ? "yes" : "no"));
    }
}