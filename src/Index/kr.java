package Index;

public class kr extends IndexSupCls {
    int k, r;
    public kr(int _k, int _r) {
        k = _k;
        r = _r;
        notation = String.format("[%d,%d]", k, r);
    }
}
