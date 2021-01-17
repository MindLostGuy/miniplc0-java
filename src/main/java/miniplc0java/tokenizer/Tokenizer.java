package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        Token res = null;
        if (Character.isDigit(peek)) {
            res = lexNum();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            res = lexIdentOrKeyword();
        } else {
            res = lexOperatorOrUnknown();
        }
        return res;
    }

    private Token lexNum() throws TokenizeError{
        String value = "";
        Pos startPos = it.currentPos();
        char peek = it.peekChar();
        while(Character.isDigit(peek)){
            value += it.nextChar();
            peek = it.peekChar();
        }
        if(peek == '.'){
            value += it.nextChar();
            peek = it.peekChar();
            if(!Character.isDigit(peek)){
                throw new TokenizeError(ErrorCode.ExpectedToken,it.currentPos());
            }
            while(Character.isDigit(peek)){
                value += it.nextChar();
                peek = it.peekChar();
            }
            if(peek == 'e' || peek == 'E'){
                value += it.nextChar();
                peek = it.peekChar();
                if(peek == '+' || peek == '-'){
                    value += it.nextChar();
                    peek = it.peekChar();
                }
                if(!Character.isDigit(peek)){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.currentPos());
                }
                while(Character.isDigit(peek)){
                    value += it.nextChar();
                    peek = it.peekChar();
                }
            }
            return new Token(TokenType.DOUBLE_LITERAL,value,startPos,it.currentPos());
        }else{
            return new Token(TokenType.UINT_LITERAL,value,startPos,it.currentPos());
        }
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        String value = "";
        Pos startPos = it.currentPos();
        char peek = it.peekChar();
        while (Character.isDigit(peek) || Character.isAlphabetic(peek) || peek == '_'){
            value += it.nextChar();
            peek = it.peekChar();
        }
        return new Token(lexIdentOrKeyword(value),value,startPos,it.currentPos());
    }

    private TokenType lexIdentOrKeyword(String value){
        switch (value){
            case "fn":return TokenType.FN_KW;
            case "let":return TokenType.LET_KW;
            case "const":return TokenType.CONST_KW;
            case "as":return TokenType.AS_KW;
            case "while":return TokenType.WHILE_KW;
            case "if":return TokenType.IF_KW;
            case "else":return TokenType.ELSE_KW;
            case "return":return TokenType.RETURN_KW;
            case "break":return TokenType.BREAK_KW;
            case "continue":return TokenType.CONTINUE_KW;
            default:return TokenType.IDENT;
        }
    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        switch (it.peekChar()) {
            case '+':
                it.nextChar();
                return new Token(TokenType.PLUS, "+", it.previousPos(), it.currentPos());

            case '-':
                it.nextChar();
                if(it.peekChar() == '>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.MINUS, "-", it.previousPos(), it.currentPos());

            case '*':
                it.nextChar();
                return new Token(TokenType.MUL, "*", it.previousPos(), it.currentPos());

            case '/':
                it.nextChar();
                if(it.peekChar() == '/'){
                    it.nextChar();
                    while(it.peekChar() != '\n'){
                        it.nextChar();
                    }
                    return nextToken();
                }
                return new Token(TokenType.DIV, "/", it.previousPos(), it.currentPos());

            case '=':
                it.nextChar();
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", it.previousPos(), it.currentPos());
                }else{
                    return new Token(TokenType.ASSIGN, "=", it.previousPos(), it.currentPos());
                }
            case '!':
                it.nextChar();
                if(it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());
                }else{
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
            case '<':
                it.nextChar();
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());
                }else{
                    return new Token(TokenType.LT, "<", it.previousPos(), it.currentPos());
                }
            case '>':
                it.nextChar();
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", it.previousPos(), it.currentPos());
                }else{
                    return new Token(TokenType.GT, ">", it.previousPos(), it.currentPos());
                }
            case '(':
                it.nextChar();
                return new Token(TokenType.L_PAREN, "(", it.previousPos(), it.currentPos());
            case ')':
                it.nextChar();
                return new Token(TokenType.R_PAREN, ")", it.previousPos(), it.currentPos());
            case '{':
                it.nextChar();
                return new Token(TokenType.L_BRACE, "{", it.previousPos(), it.currentPos());
            case '}':
                it.nextChar();
                return new Token(TokenType.R_BRACE, "}", it.previousPos(), it.currentPos());
            case ',':
                it.nextChar();
                return new Token(TokenType.COMMA, ",", it.previousPos(), it.currentPos());
            case ':':
                it.nextChar();
                return new Token(TokenType.COLON, ":", it.previousPos(), it.currentPos());
            case ';':
                it.nextChar();
                return new Token(TokenType.SEMICOLON, ";", it.previousPos(), it.currentPos());
            case '\'':
                return lexChar();
            case '"':
                return lexString();
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private Token lexChar() throws TokenizeError{
        if(it.peekChar() == '\''){
            Pos startPos = it.currentPos();
            it.nextChar();
            if(it.peekChar() == '\''){ // 连续两个 ‘
                throw new TokenizeError(ErrorCode.InvalidInput,it.currentPos());
            }else if(it.peekChar() == '\\'){ // 遇到转义符
                it.nextChar(); // 先读取 \
                String value = "\\";
                char ch = it.nextChar(); // 读取 \后面的字符
                value += ch;
                it.nextChar();
                if(ch == '\\'){
                    return new Token(TokenType.UINT_LITERAL,""+(int)'\\',startPos,it.currentPos());
                }else if(ch == '"'){
                    return new Token(TokenType.UINT_LITERAL,""+(int)'"',startPos,it.currentPos());
                }else if(ch == '\''){
                    return new Token(TokenType.UINT_LITERAL,""+(int)'\'',startPos,it.currentPos());
                }else if(ch == 'n'){
                    return new Token(TokenType.UINT_LITERAL,""+(int)'\n',startPos,it.currentPos());
                }else if(ch == 'r'){
                    return new Token(TokenType.UINT_LITERAL,""+(int)'\r',startPos,it.currentPos());
                }else if(ch == 't'){
                    return new Token(TokenType.UINT_LITERAL,""+(int)'\t',startPos,it.currentPos());
                }else{
                    throw new TokenizeError(ErrorCode.InvalidInput,it.currentPos());
                }
            }else{ // 其他正常字符
                String value = ""+ (int) it.nextChar();
                if(it.nextChar() != '\''){ // 不是 ' 结尾
                    throw new TokenizeError(ErrorCode.InvalidInput,it.currentPos());
                }
                return new Token(TokenType.UINT_LITERAL,value,startPos,it.currentPos());
            }
        }else{
            throw new TokenizeError(ErrorCode.InvalidInput,it.currentPos());
        }
    }

    private Token lexString() throws TokenizeError{
        Pos startPos = it.currentPos();
        if(it.nextChar() != '"'){
            throw new TokenizeError(ErrorCode.InvalidInput,it.currentPos());
        }
        char peek = it.nextChar();
        String value = "";
        while (peek != '"'){
            if(peek == '\\'){
                peek = it.nextChar();
                if(peek == 'n'){
                    value += '\n';
                }else if(peek == 'r'){
                    value += '\r';
                }else if(peek == 't'){
                    value += '\t';
                }else if(peek == '\\'){
                    value += '\\';
                }else if(peek == '\"'){
                    value += '"';
                }else if(peek == '\''){
                    value += '\'';
                }else{
                    throw new TokenizeError(ErrorCode.InvalidInput,it.currentPos());
                }
            }else {
                value += peek;
            }
            peek = it.nextChar();
            if(peek == 0){
                throw new TokenizeError(ErrorCode.InvalidInput,it.currentPos());
            }
        }

        return new Token(TokenType.STRING_LITERAL,value,startPos,it.currentPos());
    }
    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
