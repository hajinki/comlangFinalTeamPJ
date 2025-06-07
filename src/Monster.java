public class Monster {
    private String type;
    private int hp;
    private int damage;
   

    // GameEngine call
    public Monster(char type) {
        switch (type) {
            case 'G' -> {
                this.type = "Goblin";
                this.hp = 3;
                this.damage = 1;
            }
            case 'O' -> {
                this.type = "Orc";
                this.hp = 8;
                this.damage = 3;
            }
            case 'T' -> {
                this.type = "Troll";
                this.hp = 15;
                this.damage = 4;
            }
            default -> {
                this.type = "Unknown";
                this.hp = 1;
                this.damage = 0;
            }
        }
    }
    
    // Existing creators can also be retained if needed
    public Monster(char type, int hp) {
        this.hp = hp;
        switch (type) {
            case 'G' -> {
                this.type = "Goblin";
                this.damage = 1;
            }
            case 'O' -> {
                this.type = "Orc";
                this.damage = 3;
            }
            case 'T' -> {
                this.type = "Troll";
                this.damage = 4;
            }
            default -> {
                this.type = "Unknown";
                this.damage = 0;
            }
        }
    }

    public String getType() { return type; }
    public int getHp() { return hp; }
    public int getDamage() { return damage; }

    public void takeDamage(int amount) {
        hp -= amount;
    }

    public boolean isDead() {
        return hp <= 0;
    }
}