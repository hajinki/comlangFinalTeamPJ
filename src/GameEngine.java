import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GameEngine {
    private Room room;
    private Hero hero;

    public void start() {
        System.out.println("=== Solo Adventure Maze ===");
        try {
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
                    hero = new Hero(j, i);  // x=열, y=행
                    return;
                }
            }
        }

        // @가 없을 경우 → (1,1) 또는 빈칸 랜덤
        if (grid[1][1] == ' ') {
            hero = new Hero(1, 1);
        } else {
            outer:
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (grid[i][j] == ' ') {
                        hero = new Hero(j, i);
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
            if (input.length() == 0) continue;

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
                hero.setPosition(newX, newY);
                checkForWeaponPickup(newX, newY);

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
                    Monster monster = new Monster(c);
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
                        grid[ny][nx] = ' ';
                    }
                }
            }
        }
    }
    
    private void checkForWeaponPickup(int x, int y) throws IOException {
        char cell = room.getGrid()[y][x];
        Weapon found = null;
    
        switch (cell) {
            case 'S' -> found = new Weapon("Stick", 1);
            case 'W' -> found = new Weapon("Weak Sword", 2);
            case 'X' -> found = new Weapon("Strong Sword", 3);
        }
    
        if (found != null) {
            Weapon current = hero.getWeapon();
    
            if (current == null) {
                System.out.println("🔪 " + found.getName() + "를 주웠습니다!");
                hero.setWeapon(found);
                room.getGrid()[y][x] = ' ';
            } else {
                System.out.println("🪓 현재 무기: " + current.getName() + " (공격력: " + current.getDamage() + ")");
                System.out.println("⚔️ 발견한 무기: " + found.getName() + " (공격력: " + found.getDamage() + ")");
                System.out.print("무기를 바꾸시겠습니까? (y/n): ");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String input = reader.readLine();
    
                if (input.equalsIgnoreCase("y")) {
                    System.out.println("🔁 무기를 교체했습니다!");
                    // 방에 기존 무기 떨어뜨리기
                    char dropSymbol = switch (current.getDamage()) {
                        case 1 -> 'S';
                        case 2 -> 'W';
                        case 3 -> 'X';
                        default -> ' ';
                    };
                    room.getGrid()[y][x] = dropSymbol;
    
                    hero.setWeapon(found);
                } else {
                    System.out.println("🚫 무기를 바꾸지 않았습니다.");
                }
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

