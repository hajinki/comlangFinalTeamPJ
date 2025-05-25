public class Hero {
    private int x, y; // 위치
    private int hp;
    private Weapon weapon;
    private boolean hasKey;
    public static final int MAX_HP = 25;

    public Hero(int x, int y) {
        this.x = x;
        this.y = y;
        this.hp = 25;
        this.weapon = null;
        this.hasKey = false;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getHp() { return hp; }
    public void changeHp(int delta) {
        hp += delta;
        if (hp > 25) hp = 25;
    }

    public Weapon getWeapon() { return weapon; }
    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    public boolean hasKey() { return hasKey; }
    public void obtainKey() { this.hasKey = true; }
}
