package miniplc0java.o0file;

import miniplc0java.analyser.SymbolType;
import miniplc0java.instruction.Instruction;

import java.util.ArrayList;
import java.util.List;

public class FunctionDef {
    public int name;
    public int ret;
    public int params;
    public int locs;
    public int body_size;
    public SymbolType retType;
    public List<Instruction> instructions ;

    public FunctionDef()
    {
        this.ret = 0;
        this.params = 0;
        this.locs = 0;
        this.body_size = 0;
        this.instructions = new ArrayList<>();
    }

}
