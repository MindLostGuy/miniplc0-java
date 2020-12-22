package miniplc0java.analyser;

public class SymbolEntry {
    public String name;
    public SymbolType type;
    public boolean isConstant;
    public boolean isInitialized;
    public boolean isGlobal;
    public boolean isParam;
    public int level;
    public int stackOffset;

    public SymbolEntry(String name, SymbolType type, boolean isConstant, boolean isInitialized, boolean isGlobal, boolean isParam, int level, int stackOffset) {
        this.name = name;
        this.type = type;
        this.isConstant = isConstant;
        this.isInitialized = isInitialized;
        this.isGlobal = isGlobal;
        this.isParam = isParam;
        this.level = level;
        this.stackOffset = stackOffset;
    }

}
