package Index;

public class IJ extends IndexSupCls {
    String i, j;
    public IJ(String i, String j) {
        this.i = i;
        this.j = j;
        notation = String.format("[%s,%s]", this.i, this.j);
    }
}
