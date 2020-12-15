package miniplc0java.tokenizer;

public enum TokenType {
    /** fn */
    FN_KW,
    /** let */
    LET_KW,
    /** const */
    CONST_KW,
    /** as */
    AS_KW,
    /** while */
    WHILE_KW,
    /** if */
    IF_KW,
    /** else */
    ELSE_KW,
    /** return */
    RETURN_KW,
    /** 拓展 break */
    BREAK_KW,
    /** 拓展 continue */
    CONTINUE_KW,
    /** 无符号整数 */
    UINT_LITERAL,
    /** 字符串常量 */
    STRING_LITERAL,
    /** 拓展 浮点数常量 */
    DOUBLE_LITERAL,
    /** 拓展 字符常量 */
    CHAR_LITERAL,
    /** 标识符 */
    IDENT,
    /** 加号 */
    PLUS,
    /** 减号 */
    MINUS,
    /** 乘号 */
    MUL,
    /** 除号 */
    DIV,
    /** = */
    ASSIGN,
    /** == */
    EQ,
    /** 不等号 != */
    NEQ,
    /** < */
    LT,
    /** > */
    GT,
    /** <= */
    LE,
    /** >= */
    GE,
    /** 左括号 */
    L_PAREN,
    /** 右括号 */
    R_PAREN,
    /** { */
    L_BRACE,
    /** } */
    R_BRACE,
    /** -> */
    ARROW,
    /** , */
    COMMA,
    /** , */
    COLON,
    /** 分号 */
    SEMICOLON,
    /** 扩展 注释 */
    COMMENT,
    /** EOF */
    EOF;

    @Override
    public String toString() {
        return this.name();
    }
}
