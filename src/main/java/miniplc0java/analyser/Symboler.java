package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

import java.util.ArrayList;
import java.util.List;

public class Symboler {
    /** 符号表 */
    List<SymbolEntry> symbolTable = new ArrayList<>();

    public SymbolEntry addSymbol(String name, SymbolType type,
                          boolean isConstant, boolean isInitialized,
                          int level,
                          boolean isGlobal,boolean isParam,
                          Pos curPos) throws AnalyzeError {
        if(findSymbol(name) != null && findSymbol(name).level == level){
            // 重定义
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration,curPos);
        }
        SymbolEntry symbol =new SymbolEntry(
                name,type,isConstant,isInitialized,level,isGlobal,isParam,0);
        symbol.stackOffset = getOffset(symbol);
        symbolTable.add(symbol);
        return symbol;
    }
    public void addSymbol(SymbolEntry symbolEntry){
        symbolTable.add(symbolEntry);
    }
    public void popAllLevel(int level){
        for(int i=0;i<symbolTable.size();i++) {
            SymbolEntry symbol = symbolTable.get(i);
            if(symbol.level > level){
                symbolTable.remove(i);
                i--;
            }
        }
    }

    public String toString(){
        String res = "";
        for(int i=0;i<symbolTable.size();i++) {
            SymbolEntry symbol = symbolTable.get(i);
            res += "name = " + symbol.name +
                    "\ttype  = " + symbol.type +
                    "\tisConst = "+symbol.isConstant +
                    "\tisInit = " + symbol.isInitialized +
                    "\tlevel = " + symbol.level +
                    "\tisGlo = " + symbol.isGlobal +
                    "\tisParam = " + symbol.isParam +
                    "\toffset = " + symbol.stackOffset +
                    "\n"
            ;
        }
        return res;
    }

    // 找到这个名字的变量是啥
    public SymbolEntry findSymbol(String name){
        for(int i = symbolTable.size()-1;i>=0;i--){
            SymbolEntry symbol = symbolTable.get(i);
            if(symbol.name.equals(name)){
                return symbol;
            }
        }
        return null;
    }

    private int getOffset(SymbolEntry symbolEntry){
        int res = 0;
        // 如果是函数
        if(symbolEntry.type == SymbolType.FUN_NAME){
            for(int i=symbolTable.size()-1;i>=0;i--){
                SymbolEntry symbol = symbolTable.get(i);
                if(symbol.type == SymbolType.FUN_NAME)res++;
            }
            return res;
        }
        // 如果不是函数

        for(int i=symbolTable.size()-1;i>=0;i--){
            SymbolEntry symbol = symbolTable.get(i);
            if(symbol.type != SymbolType.FUN_NAME &&
            symbol.isGlobal == symbolEntry.isGlobal &&
            symbol.isParam == symbolEntry.isParam)
                res ++;
        }
        //System.out.println("get offset :"+symbolTable.size() +" "+ res);
        //System.out.println(toString());
        return res;
    }

}
