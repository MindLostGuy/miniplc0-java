package miniplc0java.instruction;

import miniplc0java.util.ByteOper;

public enum Operation {
    NOP(0x00),
    PUSH(0x01),
    POP(0x02),
    POPN(0x03),
    DUP(0x04),
    LOCA(0x0a),
    ARGA(0x0b),
    GLOBA(0x0c),
    LOAD8(0x10),
    LOAD16(0x11),
    LOAD32(0x12),
    LOAD64(0x13),
    STORE8(0x14),
    STORE16(0x15),
    STORE32(0x16),
    STORE64(0x17),
    ALLOC(0x18),
    FREE(0x19),
    STACK_ALLOC(0x1a),
    ADD_I(0x20),
    SUB_I(0x21),
    MUL_I(0x22),
    DIV_I(0x23),
    ADD_F(0x24),
    SUB_F(0x25),
    MUL_F(0x26),
    DIV_F(0x27),
    DIV_U(0x28),
    SHL(0x29),
    SHR(0x2a),
    AND(0x2b),
    OR(0x2c),
    XOR(0x2d),
    NOT(0x2e),
    CMP_I(0x30),
    CMP_U(0x31),
    CMP_F(0x32),
    NEG_I(0x34),
    NEG_F(0x35),
    ITOF(0x36),
    FTOI(0x37),
    SHRL(0x38),
    SET_LT(0x39),
    SET_GT(0x3a),
    BR(0x41),
    BR_FALSE(0x42),
    BR_TRUE(0x43),
    CALL(0x48),
    RET(0x49),
    CALL_NAME(0x4a),
    SCAN_I(0x50),
    SCAN_C(0x51),
    SCAN_F(0x52),
    PRINT_I(0x54),
    PRINT_C(0x55),
    PRINT_F(0x56),
    PRINT_S(0x57),
    PRINT_LN(0x58),
    PANIC(0xfe);

    private int num;
    private Operation(int value){
        this.num = value;
    }
    public byte[] getVal()
    {
        byte[] a = new byte[1];
        a[0] = (byte) num;
        return a;
    }
}
