package miniplc0java.instruction;

import java.util.Objects;

public class Instruction {
    private Operation opt;
    long x;

    public Instruction(Operation opt) {
        this.opt = opt;
        this.x = 0;
    }

    public Instruction(Operation opt, long x) {
        this.opt = opt;
        this.x = x;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Instruction that = (Instruction) o;
        return opt == that.opt && Objects.equals(x, that.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opt, x);
    }

    public Operation getOpt() {
        return opt;
    }

    public void setOpt(Operation opt) {
        this.opt = opt;
    }

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    boolean haveParam(){
        switch (opt){
            case PUSH:
            case POPN:
            case LOCA:
            case ARGA:
            case GLOBA:
            case STACK_ALLOC:
            case BR:
            case BR_FALSE:
            case BR_TRUE:
            case CALL:
            case CALL_NAME:
                return true;
            default:
                return false;
        }
    }



//    @Override
//    public String toString() {
//
//    }
}
