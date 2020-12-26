package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.o0file.FunctionDef;
import miniplc0java.o0file.OZero;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;


public final class Analyser {
    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;
    Symboler symboler = new Symboler();
    FunctionDef curFunc;
    OZero file = new OZero();

    /** 当前偷看的 token */
    Token peekedToken = null;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }


    /**
     * 查看下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     *
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     *
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     *
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    private void analyseProgram() throws CompileError {
        // program -> item*
        // item -> function | decl_stmt
        while(check(TokenType.FN_KW)||check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            switch (peek().getTokenType()){
                case FN_KW:
                    analyseFunc();
                    break;
                default:
                    analyseDecl_stmt(0);
                    break;
            }
        }
        expect(TokenType.EOF);
    }

    private void analyseFunc() throws CompileError {
        // function -> 'fn' IDENT '(' function_param_list? ')' '->' ty block_stmt
        expect(TokenType.FN_KW);
        Token tmp = expect(TokenType.IDENT);
        curFunc = new FunctionDef();
        if(symboler.checkGlo(tmp.getValueString()))
        {
            SymbolEntry FuncSymbolEntry = new SymbolEntry(tmp.getValueString(),SymbolType.FUNC_NAME,false,
                    false,true,false,0,0);
            int offset = symboler.getStackOffset(FuncSymbolEntry);
            FuncSymbolEntry.stackOffset = offset;
            symboler.addSymbol(FuncSymbolEntry);
            curFunc.name = offset;
        }
        else throw new AnalyzeError(ErrorCode.DuplicateDeclaration,tmp.getStartPos());
        expect(TokenType.L_PAREN);

        // function_param -> 'const'? IDENT ':' ty
        // function_param_list -> function_param (',' function_param)*
        while (!check(TokenType.R_PAREN)){
            curFunc.params++;
            boolean isConst = false;
            if(nextIf(TokenType.CONST_KW) != null){
                isConst = true;
            }
            Token param = expect(TokenType.IDENT);
            String paramName = (String)param.getValue();
            expect(TokenType.COLON);
            Token paramType = expect(TokenType.IDENT);
            SymbolType paramTy = ToTyTokenType(paramType);
            SymbolEntry ParamSymbol = new SymbolEntry(paramName,paramTy,isConst,false,
                    false,true,1,0);
            int offset = symboler.getStackOffset(ParamSymbol);
            ParamSymbol.stackOffset = offset;
            symboler.addSymbol(ParamSymbol);
            if(nextIf(TokenType.COMMA) == null){
                break;
            }
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        Token returnType = expect(TokenType.IDENT);
        SymbolType returnTy = ToTyTokenType(returnType);
        curFunc.retType = returnTy;
        switch (returnTy){
            case INT:
            case DOUBLE:
                curFunc.ret = 1;
                break;
            case VOID:
                curFunc.ret = 0;
                break;
        }

        //有返回值时，所有参数加偏移加1，保证在栈中返回值为arg0
        if(curFunc.ret == 1){
            for(SymbolEntry symbolEntry:symboler.SymbolTable){
                if (symbolEntry.isParam)
                    symbolEntry.stackOffset++;
            }
        }

        int insSize = instructions.size();

        analyseBlock_stmt(1);

        //对可能的返回预留指令

        //给Func填指令
        for(int i=insSize;i<instructions.size();i++){
            curFunc.instructions.add(instructions.get(i));
        }
        curFunc.body_size = curFunc.instructions.size();
        while(instructions.size() > insSize){
            instructions.remove(insSize);
        }

        file.addFunc(curFunc);
        //给函数后续的break等判断留位置
    }

    /*
    stmt ->
    expr_stmt
    | decl_stmt
    | if_stmt
    | while_stmt
    | break_stmt
    | continue_stmt
    | return_stmt
    | block_stmt
    | empty_stmt
    */
    private void analyseStmt(int level) throws CompileError{
        Token peek = peek();
        switch (peek.getTokenType())
        {
            case LET_KW:
            case CONST_KW:
                analyseDecl_stmt(level);
                break;
            case IF_KW:
                analyseIf_stmt(level);
                break;
            case WHILE_KW:
                analyseWhile_stmt(level);
                break;
            case BREAK_KW:
                analyseBreak_stmt(level);
                break;
            case CONTINUE_KW:
                analyseContinue_stmt(level);
                break;
            case RETURN_KW:
                analyseReturn_stmt(level);
                break;
            case L_BRACE:
                analyseBlock_stmt(level+1);
                break;
            case SEMICOLON:
                expect(TokenType.SEMICOLON);
                break;
            default:
                analyseExpr_stmt(level);
        }
    }

    private void analyseDecl_stmt(int level) throws CompileError {
        // decl_stmt -> let_decl_stmt | const_decl_stmt
        if (peek().getTokenType() == TokenType.LET_KW) {
            analyseLet_decl_stmt(level);
        }
        else if(peek().getTokenType() == TokenType.CONST_KW) {
            analyseConst_decl_stmt(level);
        }
    }

    private void analyseLet_decl_stmt(int level) throws CompileError {
        //let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
        expect(TokenType.LET_KW);
        Token token = expect(TokenType.IDENT);
        String name = (String)token.getValue();
        expect(TokenType.COLON);
        Token type = expect(TokenType.IDENT);
        SymbolType ty = ToTyTokenType(type);
        if(ty == SymbolType.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType,type.getStartPos());
        boolean isGlo = false;
        if(level == 0){
            isGlo = true;
        }
        SymbolEntry symbolEntry = new SymbolEntry(name,ty,false,false,
                isGlo,false,level,0);
        int offset = symboler.getStackOffset(symbolEntry);
        symbolEntry.stackOffset = offset;
        symboler.addSymbol(symbolEntry);
        if(nextIf(TokenType.ASSIGN) != null){
            symbolEntry.isInitialized = true;
            //存储变量域
            analyseExpr();
            //存储变量值
            //判断是否是同一类型返回值
        }
        if(level > 0)
        {
            curFunc.locs = Math.max(curFunc.locs, offset);
        }
        expect(TokenType.SEMICOLON);
    }

    private void analyseConst_decl_stmt(int level) throws CompileError {
        //const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
        expect(TokenType.CONST_KW);
        Token token = expect(TokenType.IDENT);
        String name = (String)token.getValue();
        expect(TokenType.COLON);
        Token type = expect(TokenType.IDENT);
        SymbolType ty = ToTyTokenType(type);
        if(ty == SymbolType.VOID)
            throw new AnalyzeError(ErrorCode.InvalidType,type.getStartPos());
        boolean isGlo = false;
        if(level == 0){
            isGlo = true;
        }
        SymbolEntry symbolEntry = new SymbolEntry(name,ty,true,false,
                isGlo,false,level,0);
        int offset = symboler.getStackOffset(symbolEntry);
        symbolEntry.stackOffset = offset;
        symboler.addSymbol(symbolEntry);
        expect(TokenType.ASSIGN);
        symbolEntry.isInitialized = true;
            //存储变量域
        analyseExpr();
            //存储变量值
            //判断是否是同一类型返回值
        if(level > 0)
        {
            curFunc.locs = Math.max(curFunc.locs, offset);
        }
        expect(TokenType.SEMICOLON);
    }

    // if_stmt -> 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else' block_stmt)?
    private void analyseIf_stmt(int level) throws CompileError {
    }

    // block_stmt -> '{' stmt* '}'
    private void analyseBlock_stmt(int level) throws CompileError {
        expect(TokenType.L_BRACE);
        while(peek().getTokenType() != TokenType.R_BRACE)
        {
            analyseStmt(level);
        }
        expect(TokenType.R_BRACE);
        symboler.popAbove(level);
    }

    // expr_stmt -> expr ';'
    private void analyseExpr_stmt(int level) throws CompileError{
        analyseExpr();
        expect(TokenType.SEMICOLON);
    }

    // while_stmt -> 'while' expr block_stmt
    private void analyseWhile_stmt(int level) throws CompileError{
        expect(TokenType.WHILE_KW);
        analyseExpr();
        analyseBlock_stmt(level+1);
    }

    // return_stmt -> 'return' expr? ';'
    private void analyseReturn_stmt(int level) throws CompileError{
    }

    // break_stmt -> 'break' ';'
    private void analyseBreak_stmt(int level) throws CompileError{
    }

    // continue_stmt -> 'continue' ';'
    private void analyseContinue_stmt(int level) throws CompileError{
    }


    private void analyseExpr() throws CompileError
    {

    }

    private SymbolType ToTyTokenType(Token token) throws CompileError
    {
        String tokenName = (String)token.getValue();
        switch (tokenName) {
            case "int":
                return SymbolType.INT;
            case "double":
                return SymbolType.DOUBLE;
            case "void":
                return SymbolType.VOID;
            default:
                throw new AnalyzeError(ErrorCode.InvalidType,token.getStartPos());
        }
    }

}
