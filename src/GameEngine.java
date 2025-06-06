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
    private boolean isNewGame = true; // 새 게임 시작 시 true


    public void start() {
        System.out.println("=== Solo Adventure Maze ===");
        try {
            isNewGame = true; 
            initializeDoorLinks();
            room = new Room("data/room1.csv");
            placeHero(); // @ 위치 찾기 또는 (1,1) 또는 랜덤
            gameLoop();
        } catch (IOException e) {
            System.out.println("방 로딩 실패: " + e.getMessage());
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
        String input;
    
        while (true) {
            printStatus();
            updateGrid();
            room.printRoom();
    
            checkForCombat();
    
            System.out.print("명령어 (u/d/l/r): ");
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
                    System.out.println("잘못된 명령입니다.");
                    continue;
                }
            }
    
            if (!canMoveTo(newX, newY)) {
                System.out.println("❌ 이동할 수 없습니다.");
                continue;
            }
    
            char[][] grid = room.getGrid();
            char tile = grid[newY][newX];
            if (tile == 'D' && !hero.hasKey()) {
                System.out.println("🔒 열쇠가 없어 마스터 도어로 이동할 수 없습니다!");
                continue;  // 이동 막고 명령 재입력
            }

    
            // 문이라면 tryDoor() 실행
            if (tile == 'D' || tile == 'd') {
                if (tryDoor(newX, newY)) continue;
            }
    
            // 몬스터라면 막기
            if (tile == 'G' || tile == 'T' || tile == 'o') {
                System.out.println("❗ 몬스터가 길을 막고 있습니다! 공격하시겠습니까?");
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
                    System.out.println("🗡 무기를 획득했습니다: " + found.getName());
                    grid[newY][newX] = ' ';
                } else {
                    System.out.println("🗡 무기 '" + found.getName() + "' 을 발견했습니다! 현재 무기: " + hero.getWeapon().getName());
                    System.out.print("이 무기로 교체하시겠습니까? (y/n): ");
                    input = reader.readLine();
                    if (input.equalsIgnoreCase("y")) {
                        hero.setWeapon(found);
                        grid[newY][newX] = ' ';
                        System.out.println("🗡 무기를 " + found.getName() + " 으로 교체했습니다!");
                    } else {
                        System.out.println("❌ 무기 교체를 취소했습니다.");
                        // 무기 거절 시 해당 위치에 원래 무기 심볼 복구
                        char weaponSymbol = switch (found.getName()) {
                            case "Stick" -> 'S';
                            case "Weak Sword" -> 'W';
                            case "Strong Sword" -> 'X';
                            default -> ' ';
                        };
                        grid[newY][newX] = weaponSymbol;
                        continue; // 이동하지 않음
                    }
                }
            }
    
            if (!checkForPotion(newX, newY)) {
                continue;  // 포션은 있지만 체력 만땅이라 안 마심 → 이동하지 않음
            }
    
            // 이동 처리
            grid[hero.getY()][hero.getX()] = ' ';  // 현재 위치 비움
            hero.setPosition(newX, newY);          // 위치 갱신
            grid[newY][newX] = '@';                // 새로운 위치에 '@'
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
                    // ✅ 기존에 만들어둔 몬스터가 있는지 확인
                    Monster monster = room.getMonsterAt(nx, ny);
    
                    // ✅ 없으면 새로 만들어서 room에 등록
                    if (monster == null) {
                        monster = new Monster(c);
                        room.setMonsterAt(nx, ny, monster);
                    }
    
                    // ✅ 전투 시작
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

                    while (true) {
                        System.out.println("🧟 몬스터 발견! 종류: " + monster.getType() + " | HP: " + monster.getHp());
                        System.out.print("공격하시겠습니까? (y/n): ");
                        String input = reader.readLine();

                        if (input.equalsIgnoreCase("y")) {
                            if (hero.getWeapon() == null) {
                                System.out.println("⚠ 무기가 없어 공격할 수 없습니다!");
                                break;
                            }

                            monster.takeDamage(hero.getWeapon().getDamage());
                            hero.changeHp(-monster.getDamage());

                            System.out.println("💥 당신이 " + monster.getDamage() + " 피해를 입었습니다.");
                            System.out.println("⚔️ 당신이 " + monster.getType() + "에게 " + hero.getWeapon().getDamage() + " 피해를 입혔습니다!");
                            System.out.println("❤️ 현재 HP: " + hero.getHp());


                            
                            if (hero.getHp() <= 0) {
                                System.out.println("☠ 당신은 쓰러졌습니다. 게임 오버!");
                                System.exit(0);
                            }

                            if (monster.isDead()) {
                                System.out.println("✅ 몬스터 처치 성공!");
                                if (c == 'T') {
                                    hero.obtainKey();
                                    System.out.println("🗝 열쇠를 얻었습니다!");
                                }
                                grid[ny][nx] = ' ';
                                room.setMonsterAt(nx, ny, null);
                                updateGrid();
                                room.printRoom();
                                break; // 전투 종료
                            } else {
                                // 몬스터가 아직 살아있을 때만 HP 표시
                                System.out.println("🩸 몬스터 HP: " + monster.getHp());
                                

                                if (hero.getHp() <= 5) {
                                    System.out.println("⚠️ 체력이 매우 낮습니다! 포션을 사용하거나 전투를 피하세요!");
                                }
                                room.printRoom();
                            }

                        } else if (input.equalsIgnoreCase("n")) {
                            System.out.println("👉 전투를 회피했습니다.");
                            break;
                        } else {
                            System.out.println("잘못된 입력입니다. y 또는 n을 입력하세요.");
                        }
                    }

                }
            }
        }
    }
    
    
    // private void checkForWeaponPickup(int x, int y) throws IOException {
    //     char[][] grid = room.getGrid();
    //     char cell = grid[y][x];
    
    //     Weapon found = switch (cell) {
    //         case 'S' -> new Weapon("Stick", 1);
    //         case 'W' -> new Weapon("Weak Sword", 2);
    //         case 'X' -> new Weapon("Strong Sword", 3);
    //         default -> null;
    //     };

        // int prevX = hero.getX();
        // int prevY = hero.getY();


        // if (found != null) {
        //     if (hero.getWeapon() == null) {
        //         // 무기 없을 때는 바로 장착
        //         hero.setWeapon(found);
        //         System.out.println("🗡 무기를 획득했습니다: " + found.getName());
        //         grid[y][x] = ' ';
        //     } else {
        //         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        //         System.out.println("🗡 무기 '" + found.getName() + "' 을 발견했습니다! 현재 무기: " + hero.getWeapon().getName());
        //         System.out.print("이 무기로 교체하시겠습니까? (y/n): ");
        //         String input = reader.readLine();
        
        //         if (input.equalsIgnoreCase("y")) {
        //             hero.setWeapon(found);
        //             grid[y][x] = ' ';
        //             System.out.println("🗡 무기를 " + found.getName() + " 으로 교체했습니다!");
        //         } else {
        //             System.out.println("❌ 무기 교체를 취소했습니다.");
        
        //             // 무기 심볼 복구
        //             char weaponSymbol = switch (found.getName()) {
        //                 case "Stick" -> 'S';
        //                 case "Weak Sword" -> 'W';
        //                 case "Strong Sword" -> 'X';
        //                 default -> ' ';
        //             };
        //             grid[y][x] = weaponSymbol;
        
        //             // ✅ 영웅을 원래 위치로 되돌리기
        //             grid[y][x] = weaponSymbol;            // 현재 자리에 무기 다시
        //             grid[prevY][prevX] = '@';             // 이전 자리에 영웅 다시 배치
        //             hero.setPosition(prevX, prevY);       // 좌표도 롤백
        
        //             // 방 저장
        //             room.saveToFile("save/" + room.getFilename());
        //             return; // 이동 중단
        //         }
        
        //         room.saveToFile("save/" + room.getFilename()); // 무기 변경 후에도 저장
        //     }
        // }
        
    
    //     if (found != null) {
    //         if (hero.getWeapon() == null) {
    //             hero.setWeapon(found);
    //             System.out.println("🗡 무기를 획득했습니다: " + found.getName());
    //             grid[y][x] = ' ';
    //         } else {
    //             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    //             System.out.println("🗡 무기 '" + found.getName() + "' 을 발견했습니다! 현재 무기: " + hero.getWeapon().getName());
    //             System.out.print("이 무기로 교체하시겠습니까? (y/n): ");
    //             String input = reader.readLine();
    //             if (input.equalsIgnoreCase("y")) {
    //                 hero.setWeapon(found);
    //                 grid[y][x] = ' ';
    //                 System.out.println("🗡 무기를 " + found.getName() + " 으로 교체했습니다!");
    //                   } else {
    //                 System.out.println("❌ 무기 교체를 취소했습니다.");
                
    //                 // 💡 무기 거절 시 원래 무기 심볼 복구
    //                 char weaponSymbol = switch (found.getName()) {
    //                     case "Stick" -> 'S';
    //                     case "Weak Sword" -> 'W';
    //                     case "Strong Sword" -> 'X';
    //                     default -> ' ';
    //                 };
    //                 grid[y][x] = weaponSymbol;
    //                 }
                
    //         }
    //     }
    // }

    // GameEngine 필드에 추가
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
    // addDoorLink("room2.csv", new Point(5, 0), "room3.csv", new Point(0, 0));
    // addDoorLink("room3.csv", new Point(0, 0), "room2.csv", new Point(5, 0));
    // addDoorLink("room4.csv", new Point(0, 5), "room3.csv", new Point(4, 4));  // 일반문 d
    // addDoorLink("room3.csv", new Point(4, 4), "room4.csv", new Point(0, 5));

    addDoorLink("room4.csv", new Point(0, 5), "room3.csv", new Point(0, 0));
    addDoorLink("room3.csv", new Point(0, 0), "room4.csv", new Point(0, 5));

    addDoorLink("room4.csv", new Point(4, 0), "room3.csv", new Point(0, 0));  // 마스터도어 D
    addDoorLink("room3.csv", new Point(0, 0), "room4.csv", new Point(4, 0));

    // room3 (4,4) <-> room4 (4,0)
    // addDoorLink("room3.csv", new Point(0, 0), "room4.csv", new Point(5, 0)); // 이게 핵심!
    // addDoorLink("room4.csv", new Point(5, 0), "room3.csv", new Point(0, 0));

    // addDoorLink("room3.csv", new Point(4, 4), "room4.csv", new Point(4, 0));
    // addDoorLink("room4.csv", new Point(4, 0), "room3.csv", new Point(4, 4));

}


private void addDoorLink(String fromRoom, Point fromPos, String toRoom, Point toPos) {
    doorMap.computeIfAbsent(fromRoom, k -> new HashMap<>())
           .put(fromPos, new DoorLink(toRoom, toPos));
}


    
    private boolean tryDoor(int x, int y) throws IOException {
        char tile = room.getGrid()[y][x];
        // String targetFilename = room.getDoorFilenameAt(x, y);
        String currentRoomName = new File(room.getPath()).getName(); // 예: room1.csv
        DoorLink link = doorMap.getOrDefault(currentRoomName, new HashMap<>()).get(new Point(x, y));
        
        if (link == null) {
            System.out.println("문에 연결된 파일이 없습니다.");
            return false;
        }
        // ✅ Master door (D): 열쇠 필요
        if (tile == 'D' && !hero.hasKey()) {
            System.out.println("🚪 마스터 도어입니다. 열쇠가 필요합니다!");
            return false;
        }
        
        if (room.getPath().contains("room4.csv") && x == 0 && y == 5) {
            System.out.println("🎉 축하합니다! 마스터 도어를 열고 게임을 클리어했습니다!");
            System.exit(0); // 게임 종료
            return true;
        }

        
        
        String targetFilename = link.filename;
        Point newHeroPos = link.position;
    

        String currentPath = room.getPath();  // 현재 방의 경로 (예: data/room1.csv)

        
    
        System.out.println("🚪 문을 열고 다음 방으로 이동합니다!");
    

        // ✅ 현재 방 상태 저장
        String currentSavePath = room.getPath().replace("data/", "save/");
        FileManager.saveRoom(currentSavePath, room.getGrid());

        // ✅ 다음 방 정보 준비
        String nextRoomName = link.filename;
        String dataPath = "data/" + nextRoomName;
        String savePath = "save/" + nextRoomName;
        
        File saveFile = new File(savePath);
        
        // ✅ save 폴더에 방이 없다면 → 처음 방문이므로 data에서 복사
    if (!saveFile.exists()) {
        Room tempRoom = new Room(dataPath);           // data에서 최초 로드
        FileManager.saveRoom(savePath, tempRoom.getGrid()); // save에 저장
    }

    // ✅ save에서 방을 로드
    room = new Room(savePath);
    isNewGame = true;  // 다음 방 입장 → 위치 재배치 필요
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
                System.out.println("🧪 포션을 마셨습니다! HP: " + before + " → " + hero.getHp());
                grid[y][x] = ' ';  // 포션 제거
            } else {
                System.out.println("🧪 포션을 발견했지만 HP가 가득 차 있어 남겨두었습니다.");
                return false;  // 이동하지 않음
            }
        }
        return true;  // 이동 가능
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
