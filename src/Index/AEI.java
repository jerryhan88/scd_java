package Index;

public class AEI extends IndexSupCls {
    int k, r;
    String i;
    public AEI(int _k, int _r, String _i) {
        k = _k;
        r = _r;
        i = _i;
        notation = String.format("[%d,%d,%s]", k, r, i);
    }

    public String get_label() {
        return String.format("%d&%d&%s", k, r, i);
    }
}
