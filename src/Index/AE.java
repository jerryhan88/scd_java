package Index;

public class AE extends IndexSupCls {
    private int a, e;
    public AE(int a, int e) {
        this.a = a;
        this.e = e;
        set_label();
    }

    public AE(int a, Object e) {
        this.a = a;
        this.e = (Integer) e;
        set_label();
    }

    private void set_label() {
        label = String.format("%d&%d", this.a, this.e);
    }
}
