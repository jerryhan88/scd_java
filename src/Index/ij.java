package Index;

public class ij extends IndexSupCls {
    String i, j;
    public ij(String _i, String _j) {
        i = _i;
        j = _j;
        notation = String.format("[%s,%s]", i, j);
    }
}
