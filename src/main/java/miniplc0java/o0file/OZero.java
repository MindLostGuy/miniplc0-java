package miniplc0java.o0file;

import java.util.ArrayList;
import java.util.List;

public class OZero {
    byte[] magic ={0x72,0x30,0x3b,0x3e};
    byte[] version ={0x00,0x00,0x00,0x01};
    int gol_len;
    List<GlobalDef> globalDefList;
    int func_len;
    List<FunctionDef> functionDefList;

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

    public FunctionDef findFunc(String name)
    {
        for(FunctionDef functionDef:functionDefList){
            if(functionDef.NAME.equals(name))
                return functionDef;
        }
        return null;
    }

}
