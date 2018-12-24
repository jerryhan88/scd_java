package Index;

public class IJ extends IndexSupCls {
    private String i, j;
    public IJ(String i, String j) {
        this.i = i;
        this.j = j;
        set_label();
    }

    public IJ(Object i, Object j) {
        this.i = (String) i;
        this.j = (String) j;
        set_label();
    }

    private void set_label() {
        label = String.format("%s&%s", this.i, this.j);
    }

}
