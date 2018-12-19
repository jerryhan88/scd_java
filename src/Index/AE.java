package Index;

public class AE extends IndexSupCls {
    int a, e;
    public AE(int a, int e) {
        this.a = a;
        this.e = e;
        notation = String.format("[%d,%d]", this.a, this.e);
    }
}
