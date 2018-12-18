package Index;

public class AEK extends IndexSupCls {
    int a, e, k;
    public AEK(int _a, int _e, int _k) {
        a = _a;
        e = _e;
        k = _k;
        notation = String.format("[%d,%d,%d]", a, e, k);
    }

    public String get_label() {
        return String.format("%d&%d&%d", a, e, k);
    }
}
