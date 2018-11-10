package Index;

public class AE extends IndexSupCls {
    int a, e;
    public AE(int _a, int _e) {
        a = _a;
        e = _e;
        notation = String.format("[%d,%d]", a, e);
    }
}
