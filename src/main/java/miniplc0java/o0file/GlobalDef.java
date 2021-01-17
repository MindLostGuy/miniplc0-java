package miniplc0java.o0file;

import miniplc0java.analyser.SymbolType;
import miniplc0java.analyser.TyType;

public class GlobalDef {
    public boolean isConst;
    public int value_len;
    public String value;


    public GlobalDef(boolean isConst, String value) {
        this.isConst = isConst;
        this.value = value;
        this.value_len = value.length();
    }
}
