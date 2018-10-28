package Index;

import java.io.Serializable;

public class IndexSupCls implements Serializable {
    String notation;

    public String toString() {
        return notation;
    }

    public boolean equals(Object _o) {
        IndexSupCls o = (IndexSupCls) _o;
        return this.notation.equals(o.notation);
    }

    public int hashCode() {
        return notation.hashCode();
    }
}
