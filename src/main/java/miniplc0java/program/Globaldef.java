package miniplc0java.program;

public class Globaldef {
    boolean isConst;
    String value;

    public Globaldef(boolean isConst, String value) {
        this.isConst = isConst;
        this.value = value;
    }

    @Override
    public String toString() {
        return "\n\tisConst=" + isConst +
                "\tvalue=" + value;
    }
}
