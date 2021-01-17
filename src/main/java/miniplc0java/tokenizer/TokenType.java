package miniplc0java.tokenizer;

public enum TokenType {
    //关键字
    FN_KW,
    LET_KW ,
    CONST_KW,
    AS_KW,
    WHILE_KW,
    IF_KW,
    ELSE_KW,
    RETURN_KW,
    BREAK_KW,
    CONTINUE_KW,

    // 字面量
    UINT_LITERAL,
    STRING_LITERAL,
    DOUBLE_LITERAL,
    CHAR_LITERAL,

    // 标识符
    IDENT,

    // 运算符
    PLUS   ,//   -> '+'
    MINUS  ,//   -> '-'
    MUL    ,//   -> '*'
    DIV    ,//   -> '/'
    ASSIGN ,//   -> '='
    EQ     ,//   -> '=='
    NEQ    ,//   -> '!='
    LT     ,//   -> '<'
    GT     ,//   -> '>'
    LE     ,//   -> '<='
    GE     ,//   -> '>='
    L_PAREN ,//  -> '('
    R_PAREN ,//  -> ')'
    L_BRACE ,//  -> '{'
    R_BRACE ,//  -> '}'
    ARROW   ,//  -> '->'
    COMMA   ,//  -> ','
    COLON   ,//  -> ':'
    SEMICOLON ,//-> ';'

    EOF

}
