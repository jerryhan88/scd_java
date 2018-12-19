package Index;

public class AEK extends IndexSupCls {
    int a, e, k;
    public AEK(int a, int e, int k) {
        this.a = a;
        this.e = e;
        this.k = k;
        notation = String.format("[%d,%d,%d]", this.a, this.e, this.k);
    }

    public String get_label() {
        return String.format("%d&%d&%d", a, e, k);
    }
}
