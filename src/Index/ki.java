package Index;

public class ki extends IndexSupCls {
    int k, i;
    public ki(int _k, int _i) {
        k = _k;
        i = _i;
        notation = String.format("[%d,%d]", k, i);
    }

    public String get_label() {
        return String.format("%d&%d", k, i);
    }
}
