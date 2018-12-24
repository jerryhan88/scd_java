package Index;

import java.io.Serializable;

public class IndexSupCls implements Serializable {
    String label;

    public String toString() {
        return label;
    }

    public boolean equals(Object _o) {
        IndexSupCls o = (IndexSupCls) _o;
        return this.label.equals(o.label);
    }

    public int hashCode() {
        return label.hashCode();
    }

    public String get_label() {
        return label;
    }
}
