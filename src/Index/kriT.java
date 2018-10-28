package Index;

public class kriT extends IndexSupCls {
    int k, r, i;
    public kriT(int _k, int _r, int _i) {
        k = _k;
        r = _r;
        i = _i;
        notation = String.format("[%d,%d,%d]", k, r, i);
    }

    public String get_label() {
        return String.format("%d&%d&%d", k, r, i);
    }
}
