public class Weapon {
    private String name;
    private int damage;

    public Weapon(String name, int damage) {
        this.name = name;
        this.damage = damage;
    }

    public String getName() { return name; }
    public int getDamage() { return damage; }

    public static Weapon fromChar(char c) {
        return switch (c) {
            case 'S' -> new Weapon("Stick", 1);
            case 'W' -> new Weapon("Weak Sword", 2);
            case 'X' -> new Weapon("Strong Sword", 3);
            default -> null;
        };
    }
}
