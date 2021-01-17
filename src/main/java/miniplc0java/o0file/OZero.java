package miniplc0java.o0file;

import miniplc0java.util.ByteOper;

import java.util.ArrayList;
import java.util.List;

public class OZero {
    byte[] magic ={0x72,0x30,0x3b,0x3e};
    byte[] version ={0x00,0x00,0x00,0x01};
    int gol_len;
    public List<GlobalDef> globalDefList;
    int func_len;
    public List<FunctionDef> functionDefList;

    public OZero()
    {
        globalDefList = new ArrayList<>();
        functionDefList =new ArrayList<>();
        gol_len = 0;
        func_len = 0;
    }

    public void addFunc(FunctionDef def)
    {
        functionDefList.add(def);
    }

    public void addGlo(GlobalDef def) { globalDefList.add(def); }

    public FunctionDef findFunc(String name)
    {
        for(FunctionDef functionDef:functionDefList){
            if(functionDef.NAME.equals(name))
                return functionDef;
        }
        return null;
    }

    public byte[] toBytes(){
        byte[] bytes = new byte[0];
        bytes = ByteOper.byteMerger(bytes,magic);
        bytes = ByteOper.byteMerger(bytes,version);
        bytes = ByteOper.byteMerger(bytes,ByteOper.toBytes(globalDefList.size(),4));
        for(GlobalDef globaldef:globalDefList){
            bytes = ByteOper.byteMerger(bytes,ByteOper.toBytes(globaldef.isConst?1:0,1));
            bytes = ByteOper.byteMerger(bytes,ByteOper.toBytes(globaldef.value_len,4));
            bytes = ByteOper.byteMerger(bytes,ByteOper.toBytes(globaldef.value));
        }
        bytes = ByteOper.byteMerger(bytes,ByteOper.toBytes(functionDefList.size(),4));
        for(FunctionDef fun:functionDefList){
            bytes = ByteOper.byteMerger(bytes,fun.toBytes());
        }
        return bytes;
    }

}
