package Index;

public class AEK extends IndexSupCls {
    int k, r, i;
    public AEK(int _k, int _r, int _i) {
        k = _k;
        r = _r;
        i = _i;
        notation = String.format("[%d,%d,%d]", k, r, i);
    }

    public String get_label() {
        return String.format("%d&%d&%d", k, r, i);
    }
}
