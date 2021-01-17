package miniplc0java.analyser;

public class SymbolEntry {

    String name;
    SymbolType type;
    boolean isConstant;
    boolean isInitialized;
    int level;
    boolean isGlobal;
    boolean isParam;
    int stackOffset;

    public SymbolEntry(String name,SymbolType type, boolean isConstant,
                       boolean isInitialized,int level,
                       boolean isGlobal,boolean isParam,int stackOffset) {
        this.name = name;
        this.type = type;
        this.isConstant = isConstant;
        this.isInitialized = isInitialized;
        this.level = level;
        this.isGlobal = isGlobal;
        this.isParam = isParam;
        this.stackOffset = stackOffset;
    }

    @Override
    public String toString() {
        return "SymbolEntry{" +
                "name='" + name + '\'' +
                "\ttype=" + type +
                "\tisConst=" + isConstant +
                "\tisInit=" + isInitialized +
                "\tlevel=" + level +
                "\tisGlo=" + isGlobal +
                "\tisParam=" + isParam +
                "\tOffset=" + stackOffset +
                '}';
    }

    /**
     * @return the stackOffset
     */
    public int getStackOffset() {
        return stackOffset;
    }

    /**
     * @return the isConstant
     */
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * @return the isInitialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * @param isConstant the isConstant to set
     */
    public void setConstant(boolean isConstant) {
        this.isConstant = isConstant;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    /**
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(int stackOffset) {
        this.stackOffset = stackOffset;
    }
}
