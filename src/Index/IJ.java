package Index;

public class IJ extends IndexSupCls {
    String i, j;
    public IJ(String _i, String _j) {
        i = _i;
        j = _j;
        notation = String.format("[%s,%s]", i, j);
    }
}
