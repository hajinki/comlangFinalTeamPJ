public class Potion {
    private int healAmount;

    public Potion(char type) {
        if (type == 'm') healAmount = 6;
        else if (type == 'B') healAmount = 12;
        else healAmount = 0;
    }

    public int getHealAmount() {
        return healAmount;
    }
}
