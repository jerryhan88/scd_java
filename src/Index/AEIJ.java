package Index;

public class AEIJ extends IndexSupCls {
    int a, e;
    String i, j;
    public AEIJ(int _a, int _e, String _i, String _j) {
        a = _a;
        e = _e;
        i = _i;
        j = _j;
        notation = String.format("[%d,%d,%s,%s]", a, e, i, j);
    }

    public String get_label() {
        return String.format("%d&%d&%s&%s", a, e, i, j);
    }
}
