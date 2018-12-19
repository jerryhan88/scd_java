package Index;

public class AEI extends IndexSupCls {
    int k, r;
    String i;
    public AEI(int k, int r, String i) {
        this.k = k;
        this.r = r;
        this.i = i;
        notation = String.format("[%d,%d,%s]", this.k, this.r, this.i);
    }

    public String get_label() {
        return String.format("%d&%d&%s", k, r, i);
    }
}
