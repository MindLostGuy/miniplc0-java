package miniplc0java.analyser;

import java.util.ArrayList;
import java.util.List;

public class Symboler {
    public List<SymbolEntry> SymbolTable = new ArrayList<>();

    public boolean checkGlo(String string)
    {
        //建立在无函数与变量重名的前提支下
        for(SymbolEntry symbolEntry:SymbolTable)
        {
            if(symbolEntry.isGlobal){
                if(string.equals(symbolEntry.name)){
                    return false;
                }
            }
            continue;
        }
        return true;
    }

    public boolean checkLoc(String name,int level)
    {
        if(findCloestSymbol(name) == null)
            return true;
        if(findCloestSymbol(name).level == level)
            return false;
        return true;
    }

    public void addSymbol(SymbolEntry symbolEntry)
    {
        SymbolTable.add(symbolEntry);
    }

    public SymbolEntry findCloestSymbol(String name)
    {
        for(int i = SymbolTable.size()-1;i >= 0; i--)
        {
            SymbolEntry symbol = SymbolTable.get(i);
            if(symbol.name.equals(name)){
                return symbol;
            }
        }
        return null;
    }

    public int getStackOffset(SymbolEntry symbolEntry)
    {
        int offset = 0;
        //函数的offset返回其在全局变量中的位置
        for(int i=SymbolTable.size()-1;i>=0;i--){
            SymbolEntry symbol = SymbolTable.get(i);
            if(symbol.isGlobal == symbolEntry.isGlobal &&
                    symbol.isParam == symbolEntry.isParam)
                offset ++;
        }
        return offset;
    }

}
