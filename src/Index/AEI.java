package Index;

public class AEI extends IndexSupCls {
    private int a, e;
    private String i;
    public AEI(int a, int e, String i) {
        this.a = a;
        this.e = e;
        this.i = i;
        set_label();
    }

    public AEI(int a, Object e, Object i) {
        this.a = a;
        this.e = (Integer) e;
        this.i = (String) i;
        set_label();
    }

    private void set_label() {
        label = String.format("%d&%d&%s", this.a, this.e, this.i);
    }
}
