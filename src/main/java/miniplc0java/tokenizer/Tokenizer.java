package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;

import java.util.regex.Pattern;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }
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
        if (Character.isDigit(peek)) {
            return lexUIntORDouble();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            return lexIdentOrKeyword();
        } else if (peek == '"'){
            return lexStringLiteral();
        } else if (peek == '\'') {
            return lexCharLiteral();
        } else {
            return lexOperatorOrUnknown();
        }
    }


    private Token lexCharLiteral() throws TokenizeError {
        String s= "";
        char peek;
        it.nextChar();
        Token token=new Token(TokenType.CHAR_LITERAL, "", it.currentPos(), it.currentPos());
        while(!it.isEOF()){
            peek = it.peekChar();
            if(peek != '\'' && peek != '\r' && peek != '\n' && peek != '\t'){
                if(peek == '\\'){
                    it.nextChar();
                    peek = it.peekChar();
                    if(peek != '\"' && peek != '\\' && peek != '\'' && peek != 'n' && peek != 't' && peek != 'r'){
                        throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                    }
                    else {
                        switch (peek) {
                            case '\\':
                                s += '\\';
                                break;
                            case '\"':
                                s += '\"';
                                break;
                            case '\'':
                                s += '\'';
                                break;
                            case 'n':
                                s += '\n';
                                break;
                            case 't':
                                s += '\t';
                                break;
                            case 'r':
                                s += '\r';
                                break;
                        }
                    }
                }
                else{
                    s+= peek;
                }
                it.nextChar();
            }
            if(it.nextChar() != '\''){
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
            else {
                break;
            }
        }
        token.setValue(s);
        token.setEndPos(it.currentPos());
        return token;
    }

    private Token lexStringLiteral() throws TokenizeError {
        String s="";
        char peek;
        it.nextChar();
        Token token=new Token(TokenType.STRING_LITERAL, "", it.currentPos(), it.currentPos());
        while(!it.isEOF()){
            peek = it.peekChar();
            if(peek != '"' && peek != '\r' && peek != '\n' && peek != '\t'){
                if(peek == '\\'){
                    it.nextChar();
                    peek = it.peekChar();
                    if(peek != '"' && peek != '\\' && peek != '\'' && peek != 'n' && peek != 't' && peek != 'r'){
                        throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                    }
                    else {
                        switch (peek){
                            case '\\':
                                s+= '\\';
                                break;
                            case '"':
                                s+= '\"';
                                break;
                            case '\'':
                                s+= '\'';
                                break;
                            case 'n':
                                s+= '\n';
                                break;
                            case 't':
                                s+= '\t';
                                break;
                            case 'r':
                                s+= '\r';
                                break;
                        }
                        it.nextChar();
                    }
                }
                else{
                    s+= peek;
                    it.nextChar();
                }
            }
            else if(peek == '"'){
                it.nextChar();
                break;
            }
            else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
        token.setValue(s);
        token.setEndPos(it.currentPos());
        return token;
    }

    private Token lexUIntORDouble() throws TokenizeError {
        //对浮点数的后续附加
        String digit="";
        Token token=new Token(TokenType.UINT_LITERAL, "", it.currentPos(), it.currentPos());
        while(!it.isEOF() && Character.isDigit(it.peekChar())){
            digit+=it.peekChar();
            it.nextChar();
        }
        if(it.peekChar() == '.'){
            digit += it.nextChar();
            token.setTokenType(TokenType.DOUBLE_LITERAL);
            char peek = it.peekChar();
            if(!Character.isDigit(peek)){
                throw new TokenizeError(ErrorCode.ExpectedToken,it.currentPos());
            }
            while(Character.isDigit(peek)){
                digit += it.nextChar();
                peek = it.peekChar();
            }
            if(peek == 'e' || peek == 'E'){
                digit += it.nextChar();
                peek = it.peekChar();
                if(peek == '+' || peek == '-'){
                    digit += it.nextChar();
                    peek = it.peekChar();
                }
                if(!Character.isDigit(peek)){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.currentPos());
                }
                while(Character.isDigit(peek)){
                    digit += it.nextChar();
                    peek = it.peekChar();
                }
            }
            token.setValue(Double.parseDouble(digit));
        }
        else {
            token.setValue(Integer.parseInt(digit));
        }
        token.setEndPos(it.currentPos());
        return token;
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        String s="";
        Token token=new Token(TokenType.IDENT, "", it.currentPos(), it.currentPos());
        while(!it.isEOF() && (Character.isLetterOrDigit(it.peekChar()) || it.peekChar() == '_')){
            s+=it.peekChar();
            it.nextChar();
        }
        token.setValue(s);
        token.setEndPos(it.currentPos());
        switch (s){
            case "fn":
                token.setTokenType(TokenType.FN_KW);
                break;

            case "let":
                token.setTokenType(TokenType.LET_KW);
                break;

            case "const":
                token.setTokenType(TokenType.CONST_KW);
                break;

            case "as":
                token.setTokenType(TokenType.AS_KW);
                break;

            case "while":
                token.setTokenType(TokenType.WHILE_KW);
                break;

            case "if":
                token.setTokenType(TokenType.IF_KW);
                break;

            case "else":
                token.setTokenType(TokenType.ELSE_KW);
                break;

            case "return":
                token.setTokenType(TokenType.RETURN_KW);
                break;

            case "break":
                token.setTokenType(TokenType.BREAK_KW);
                break;

            case "continue":
                token.setTokenType(TokenType.CONTINUE_KW);
                break;

            default:
                break;
        }
        return token;
    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());

            case '-':
                if (it.peekChar() == '>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());

            case '*':
                return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());

            case '/':
                if(it.peekChar() == '/'){
                    it.nextChar();
                    return lexCOMMENT();
                }
                return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());

            case '=':
                if (it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());

            case '!':
                if (it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());
                }
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());

            case '<':
                if (it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.LT, '<', it.previousPos(), it.currentPos());

            case '>':
                if (it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.GT, '>', it.previousPos(), it.currentPos());


            case '(':
                return new Token(TokenType.L_PAREN, '(', it.previousPos(), it.currentPos());

            case ')':
                return new Token(TokenType.R_PAREN, ')', it.previousPos(), it.currentPos());

            case '{':
                return new Token(TokenType.L_BRACE, '{', it.previousPos(), it.currentPos());

            case '}':
                return new Token(TokenType.R_BRACE, '}', it.previousPos(), it.currentPos());

            case ',':
                return new Token(TokenType.COMMA, ',', it.previousPos(), it.currentPos());

            case ':':
                return new Token(TokenType.COLON, ':', it.previousPos(), it.currentPos());

            case ';':
                return new Token(TokenType.SEMICOLON, ';', it.previousPos(), it.currentPos());

            default:
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private Token lexCOMMENT() throws TokenizeError{
        while (!it.isEOF()){
            if (it.peekChar() != '\n')
            {
                it.nextChar();
            }
            else
                break;
        }
        it.nextChar();
        return nextToken();
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
