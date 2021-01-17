package miniplc0java.program;

import miniplc0java.analyser.ExperType;
import miniplc0java.analyser.SymbolEntry;
import miniplc0java.instruction.Instruction;
import miniplc0java.util.MyByte;

import java.util.ArrayList;
import java.util.List;

public class Functiondef {
    public String name;
    public int id;
    public int name_id;
    public List<SymbolEntry> params = new ArrayList<>();
    public int returnSize;
    public ExperType returnType;
    public boolean canReturn;
    public int localSize;
    public int bodySize;
    public ArrayList<Instruction> instructions = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("\n\tname='" + name + '\'' +
                "\tnameid=" + name_id +
                "\treturnSize=" + returnSize +
                "\tparams=" + params.size() +
                "\tlocalSize=" + localSize +
                "\tbodySize=" + bodySize +
                "\tid=" + id + "\n");
        for (Instruction ins : instructions) {
            res.append("\t\t").append(ins.toString());
        }
        return res.toString();
    }

    public byte[] toBytes(){
        byte[] res = new byte[0];
        res = MyByte.merge(res,name_id,4);
        res = MyByte.merge(res,returnSize,4);
        res = MyByte.merge(res,params.size(),4);
        res = MyByte.merge(res,localSize,4);
        res = MyByte.merge(res,bodySize,4);
        for (Instruction ins : instructions) {
            res = MyByte.merge(res,ins.toBytes());
        }
        return res;
    }
}
