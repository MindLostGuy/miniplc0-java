package miniplc0java.program;

import miniplc0java.instruction.Instruction;
import miniplc0java.util.MyByte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Program {
    private byte[] magic = new byte[]{0x72,0x30,0x3b,0x3e};
    private byte[] version = new byte[]{0,0,0,1};
    public List<Globaldef> globaldefList = new ArrayList<>();
    public List<Functiondef> functiondefList = new ArrayList<>();

    public void addFunc(Functiondef functiondef){
        functiondefList.add(functiondef);
    }

    public void addGlobal(Globaldef globaldef){
        globaldefList.add(globaldef);
    }

    public Functiondef find(String name){
        for(Functiondef functiondef:functiondefList){
            if(functiondef.name.equals(name)){
                return functiondef;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "globList=" + globaldefList +
                "\nfuncList=" + functiondefList ;
    }

    public byte[] toBytes(){
        byte[] bytes = new byte[0];
        bytes = MyByte.merge(bytes,magic);
        bytes = MyByte.merge(bytes,version);
        bytes = MyByte.merge(bytes,globaldefList.size(),4);
        for(Globaldef globaldef:globaldefList){
            bytes = MyByte.merge(bytes,globaldef.isConst?1:0,1);
            bytes = MyByte.merge(bytes,globaldef.value.length(),4);
            bytes = MyByte.merge(bytes,globaldef.value);
        }
        bytes = MyByte.merge(bytes,functiondefList.size(),4);
        for(Functiondef fun:functiondefList){
            bytes = MyByte.merge(bytes,fun.toBytes());
        }
        return bytes;
    }

    public String toByteString(){
        String str = "begin\n";
        str += Arrays.toString(magic) + "\n" + Arrays.toString(version) + "\n";
        str += String.format("%08x\n",globaldefList.size());
        for(Globaldef globaldef:globaldefList){
            str += String.format("%02x\n%08x\n%s\n",
                    globaldef.isConst?1:0,globaldef.value.length()/2,globaldef.value);
        }
        str += String.format("%08x\n",functiondefList.size());
        for(Functiondef fun:functiondefList){
            str += String.format("%s\n%08x\n%08x\n%08x\n%08x\n",
                    fun.name,fun.returnSize,fun.params.size(),fun.localSize,fun.bodySize);
            for(Instruction ins : fun.instructions){
                str += ins.toByteString() + "\n";
            }
        }
        str += "\nend";
        return str;
    }
}
