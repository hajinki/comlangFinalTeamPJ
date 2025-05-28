import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;


public class GameEngine {
    private Room room;
    private Hero hero;
    private boolean isNewGame = true; // 새 게임 시작 시 true


    public void start() {
        System.out.println("=== Solo Adventure Maze ===");
        try {
            isNewGame = true; 
            room = new Room("data/room1.csv");
            placeHero(); // @ 위치 찾기 또는 (1,1) 또는 랜덤
            gameLoop();
        } catch (IOException e) {
            System.out.println("방 로딩 실패: " + e.getMessage());
        }
    }

    private void placeHero() {
        char[][] grid = room.getGrid();
        int rows = room.getRows();
        int cols = room.getCols();
    
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] == '@') {
                    if (hero == null) {
                        hero = new Hero(j, i); // 처음 실행
                    } else {
                        hero.setPosition(j, i); // 기존 Hero 재사용
                    }
                    return;
                }
            }
        }
    
        // @를 못 찾은 경우 fallback → ❗ 여기서도 hero를 새로 만들지 말고 위치만 바꾸기
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

        while (true) {
            printStatus();
            updateGrid();
            room.printRoom();

            checkForCombat();

            System.out.print("명령어 (u/d/l/r): ");
            String input = reader.readLine();
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
                    System.out.println("잘못된 명령입니다.");
                    continue;
                }
            }

            if (canMoveTo(newX, newY)) {
                // ✅ 문이라면 이동 및 tryDoor() 실행
                if (room.getGrid()[newY][newX] == 'D') {
                    hero.setPosition(newX, newY);
                    if (tryDoor(newX, newY)) continue; // 방 이동 완료되면 다음 루프 진행
                }
            
                // ✅ 문이 아니면 일반 이동
                hero.setPosition(newX, newY);
                checkForWeaponPickup(newX, newY);
                checkForPotion(newX, newY);
            } else {
                System.out.println("그 방향으로는 갈 수 없습니다.");
            }
        }

    }

    private void checkForCombat() throws IOException {
        int x = hero.getX();
        int y = hero.getY();
        char[][] grid = room.getGrid();
    
        int[][] directions = { {0,-1}, {0,1}, {-1,0}, {1,0} };
    
        for (int[] d : directions) {
            int nx = x + d[0];
            int ny = y + d[1];
    
            if (nx >= 0 && ny >= 0 && ny < room.getRows() && nx < room.getCols()) {
                char c = grid[ny][nx];
    
                if (c == 'G' || c == 'O' || c == 'T') {
                    // ✅ 기존에 만들어둔 몬스터가 있는지 확인
                    Monster monster = room.getMonsterAt(nx, ny);
    
                    // ✅ 없으면 새로 만들어서 room에 등록
                    if (monster == null) {
                        monster = new Monster(c);
                        room.setMonsterAt(nx, ny, monster);
                    }
    
                    // ✅ 전투 시작
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("몬스터 발견! 종류: " + monster.getType() + " | HP: " + monster.getHp());
                    System.out.print("공격하시겠습니까? (y/n): ");
                    String input = reader.readLine();
                    if (!input.equalsIgnoreCase("y")) continue;
    
                    if (hero.getWeapon() == null) {
                        System.out.println("무기가 없어 공격할 수 없습니다!");
                        continue;
                    }
    
                    monster.takeDamage(hero.getWeapon().getDamage());
                    hero.changeHp(-monster.getDamage());
    
                    System.out.println("👉 당신이 " + monster.getDamage() + " 피해를 입었습니다!");
                    System.out.println("👉 몬스터 HP: " + monster.getHp());
    
                    if (monster.isDead()) {
                        System.out.println("🎉 몬스터 처치 성공!");
                        if (c == 'T') {
                            hero.obtainKey();
                            System.out.println("🗝 열쇠를 얻었습니다!");
                        }
    
                        // ✅ grid랑 monsters 둘 다 비우기
                        grid[ny][nx] = ' ';
                        room.setMonsterAt(nx, ny, null);
                    }
                }
            }
        }
    }
    
    
    private void checkForWeaponPickup(int x, int y) throws IOException {
        char[][] grid = room.getGrid();
        char cell = grid[y][x];
    
        Weapon found = switch (cell) {
            case 'S' -> new Weapon("Stick", 1);
            case 'W' -> new Weapon("Weak Sword", 2);
            case 'X' -> new Weapon("Strong Sword", 3);
            default -> null;
        };
    
        if (found != null) {
            if (hero.getWeapon() == null) {
                hero.setWeapon(found);
                System.out.println("🗡 무기를 획득했습니다: " + found.getName());
                grid[y][x] = ' ';
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("🗡 무기 '" + found.getName() + "' 을 발견했습니다! 현재 무기: " + hero.getWeapon().getName());
                System.out.print("이 무기로 교체하시겠습니까? (y/n): ");
                String input = reader.readLine();
                if (input.equalsIgnoreCase("y")) {
                    hero.setWeapon(found);
                    grid[y][x] = ' ';
                    System.out.println("🗡 무기를 " + found.getName() + " 으로 교체했습니다!");
                } else {
                    System.out.println("❌ 무기 교체를 취소했습니다.");
                }
            }
        }
    }

    
    private boolean tryDoor(int x, int y) throws IOException {
        char[][] grid = room.getGrid();
        if (grid[y][x] != 'D') return false;
    
        if (room.getPath().contains("room3") && !hero.hasKey()) {
            System.out.println("🚪 문이 잠겨있습니다. 열쇠가 필요합니다!");
            return false;
        }
    
        System.out.println("🚪 문을 열고 다음 방으로 이동합니다!");
    
        // 저장
        String originalPath = room.getPath();
        String savePath = originalPath.replace("data/", "save/");
        FileManager.saveRoom(savePath, grid);
    
        // 다음 방 결정
        String nextPath = switch (originalPath) {
            case "data/room1.csv" -> "data/room2.csv";
            case "data/room2.csv" -> "data/room3.csv";
            case "data/room3.csv" -> "data/room4.csv";
            default -> null;
        };
    
        if (nextPath == null) {
            System.out.println("🎉 더 이상 이동할 방이 없습니다!");
            return false;
        }
    
        String nextSavePath = nextPath.replace("data/", "save/");
        File file = new File(nextSavePath);
    
        if (!isNewGame && file.exists()) {
            room = new Room(nextSavePath);
        } else {
            room = new Room(nextPath);
        }
    
        isNewGame = false;

        
      
        int newX = x;
        int newY = y;
        

        if (newY >= room.getRows() || newX >= room.getCols() || room.getGrid()[newY][newX] == '#') {
            // fallback: 빈 공간으로
            outer:
            for (int i = 0; i < room.getRows(); i++) {
                for (int j = 0; j < room.getCols(); j++) {
                    if (room.getGrid()[i][j] == ' ') {
                        newX = j;
                        newY = i;
                        break outer;
                    }
                }
            }
        }


    
        hero.setPosition(newX, newY);
        updateGrid();
        return true;
    }
    
    

    
    
    
    
    
    

    private void checkForPotion(int x, int y) {
        char[][] grid = room.getGrid();
        char cell = grid[y][x];
    
        int recover = switch (cell) {
            case 'm' -> 6;
            case 'B' -> 12;
            default -> 0;
        };
    
        if (recover > 0) {
            if (hero.getHp() < Hero.MAX_HP) {
                int before = hero.getHp();
                hero.changeHp(recover);
                System.out.println("🧪 포션을 마셨습니다! HP: " + before + " → " + hero.getHp());
                grid[y][x] = ' ';
            } else {
                System.out.println("🧪 포션을 발견했지만 HP가 가득 차 있어 남겨두었습니다.");
            }
        }
    }

    
    
    

    private void updateGrid() {
        char[][] grid = room.getGrid();
    
        // 기존 @ 지우기
        for (int i = 0; i < room.getRows(); i++) {
            for (int j = 0; j < room.getCols(); j++) {
                if (grid[i][j] == '@') grid[i][j] = ' ';
            }
        }
    
        int y = hero.getY();
        int x = hero.getX();
    
        // 몬스터 위에 있지 않은 경우에만 '@' 찍기
        char cell = grid[y][x];
        if (cell != 'G' && cell != 'O' && cell != 'T') {
            grid[y][x] = '@';
        }
    }
    

    private boolean canMoveTo(int x, int y) {
        if (x < 0 || y < 0 || y >= room.getRows() || x >= room.getCols()) return false;
        char cell = room.getGrid()[y][x];
        return cell != 'G' && cell != 'O' && cell != 'T';  // 몬스터는 아직 못 통과
    }

    private void printStatus() {
        System.out.println("HP: " + hero.getHp() + 
            " | 무기: " + (hero.getWeapon() != null ? hero.getWeapon().getName() : "없음") + 
            " | 열쇠: " + (hero.hasKey() ? "있음" : "없음"));
    }
}

