package Index;

public class AK extends IndexSupCls {
    int k, i;
    public AK(int _k, int _i) {
        k = _k;
        i = _i;
        notation = String.format("[%d,%d]", k, i);
    }

    public String get_label() {
        return String.format("%d&%d", k, i);
    }
}
