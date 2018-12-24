package Index;

public class AK extends IndexSupCls {
    private int a, k;
    public AK(int a, int k) {
        this.a = a;
        this.k = k;
        label = String.format("%d&%d", this.a, this.k);
    }
}
