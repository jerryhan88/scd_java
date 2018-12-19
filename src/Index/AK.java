package Index;

public class AK extends IndexSupCls {
    int k, i;
    public AK(int k, int i) {
        this.k = k;
        this.i = i;
        notation = String.format("[%d,%d]", this.k, this.i);
    }

    public String get_label() {
        return String.format("%d&%d", k, i);
    }
}
