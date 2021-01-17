package miniplc0java.o0file;

import miniplc0java.analyser.SymbolEntry;
import miniplc0java.analyser.SymbolType;
import miniplc0java.instruction.Instruction;
import miniplc0java.util.ByteOper;

import java.util.ArrayList;
import java.util.List;

public class FunctionDef {
    public String NAME;
    public int name;
    public int ret;
    public int params;
    public int locs;
    public int body_size;
    public SymbolType retType;
    public List<SymbolEntry> paramList ;
    public List<Instruction> instructions ;
    public boolean canReturn;

    public FunctionDef()
    {
        this.ret = 0;
        this.params = 0;
        this.locs = 0;
        this.body_size = 0;
        this.paramList = new ArrayList<>();
        this.instructions = new ArrayList<>();
    }


    public byte[] toBytes() {
        byte[] res = new byte[0];
        res = ByteOper.byteMerger(res, ByteOper.toBytes(name, 4));
        res = ByteOper.byteMerger(res, ByteOper.toBytes(ret, 4));
        res = ByteOper.byteMerger(res, ByteOper.toBytes(params, 4));
        res = ByteOper.byteMerger(res, ByteOper.toBytes(locs, 4));
        res = ByteOper.byteMerger(res, ByteOper.toBytes(body_size, 4));
        for (Instruction ins : instructions) {
            res = ByteOper.byteMerger(res, ins.toBytes());
        }
        return res;
    }


}
