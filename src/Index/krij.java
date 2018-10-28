package Index;

public class krij extends IndexSupCls {
    int k, r;
    String i, j;
    public krij(int _k, int _r, String _i, String _j) {
        k = _k;
        r = _r;
        i = _i;
        j = _j;
        notation = String.format("[%d,%d,%s,%s]", k, r, i, j);
    }

    public String get_label() {
        return String.format("%d&%d&%s&%s", k, r, i, j);
    }
}
