package Index;

public class AEK extends IndexSupCls {
    private int a, e, k;
    public AEK(int a, int e, int k) {
        this.a = a;
        this.e = e;
        this.k = k;
        set_label();
    }

    public AEK(int a, Object e, Object k) {
        this.a = a;
        this.e = (Integer) e;
        this.k = (Integer) k;
        set_label();
    }

    private void set_label() {
        label = String.format("%d&%d&%d", this.a, this.e, this.k);
    }

}
