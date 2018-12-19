package Index;

public class AEIJ extends IndexSupCls {
    int a, e;
    String i, j;
    public AEIJ(int a, int e, String i, String j) {
        this.a = a;
        this.e = e;
        this.i = i;
        this.j = j;
        notation = String.format("[%d,%d,%s,%s]", this.a, this.e, this.i, this.j);
    }

    public String get_label() {
        return String.format("%d&%d&%s&%s", a, e, i, j);
    }
}
