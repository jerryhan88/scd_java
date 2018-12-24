package Index;

public class AEIJ extends IndexSupCls {
    private int a, e;
    private String i, j;
    public AEIJ(int a, int e, String i, String j) {
        this.a = a;
        this.e = e;
        this.i = i;
        this.j = j;
        set_label();
    }

    public AEIJ(int a, Object e, Object i, Object j) {
        this.a = a;
        this.e = (Integer) e;
        this.i = (String) i;
        this.j = (String) j;
        set_label();
    }

    private void set_label() {
        label = String.format("%d&%d&%s&%s", this.a, this.e, this.i, this.j);
    }
}
