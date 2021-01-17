package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.o0file.FunctionDef;
import miniplc0java.o0file.GlobalDef;
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
    public OZero file = new OZero();
    Stack<SymbolType> TypeStack = new Stack<>();

    /** 当前偷看的 token */
    Token peekedToken = null;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        System.out.println("\n"+symboler);
        System.out.println(file);
        System.out.println(Arrays.toString(file.toBytes()));
        return instructions;
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
        symboler.addSymbol(new SymbolEntry("_start",SymbolType.FUNC_NAME,true,false,
                true,false,0,0));
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
        addStartFun();
        addGlo();
        expect(TokenType.EOF);
    }

    private void addGlo() throws AnalyzeError {
        for(SymbolEntry symbol:symboler.SymbolTable){
            if(symbol.isGlobal && symbol.type != SymbolType.FUNC_NAME){
                file.addGlo(new GlobalDef(symbol.isConstant,"00000000"));
            }
            else if(symbol.isGlobal){
                file.addGlo(new GlobalDef(true,symbol.name));
            }
        }
    }

    private void addStartFun() throws AnalyzeError {
        FunctionDef mainFunc = file.findFunc("main");
        if(mainFunc == null)
        {
            throw new AnalyzeError(ErrorCode.NoMain,peekedToken.getStartPos());
        }
        AddIns(Operation.CALL,mainFunc.name);
        FunctionDef functiondef = new FunctionDef();
        functiondef.NAME = "_start";
        functiondef.name = 0;
        functiondef.ret = 0;
        functiondef.locs = 0;
        functiondef.instructions.addAll(instructions);
        functiondef.body_size= instructions.size(); //(length+7)/8;
        while(instructions.size() > 0){
            instructions.remove(0);
        }
        file.functionDefList.add(0,functiondef);
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
            curFunc.NAME = tmp.getValueString();
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
            curFunc.paramList.add(ParamSymbol);
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
                analyseBreak_stmt();
                break;
            case CONTINUE_KW:
                analyseContinue_stmt();
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
            pushVAR(token,false);
            analyseExpr();
            AddIns(Operation.STORE64);
            if(TypeStack.pop() != ty)
            {
                throw new AnalyzeError(ErrorCode.InvalidTypeComparion,peekedToken.getStartPos());
            }
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
        pushVAR(token,false);
        analyseExpr();
        AddIns(Operation.STORE64);
        if(TypeStack.pop() != ty)
        {
            throw new AnalyzeError(ErrorCode.InvalidTypeComparion,peekedToken.getStartPos());
        }
        if(level > 0)
        {
            curFunc.locs = Math.max(curFunc.locs, offset);
        }
        expect(TokenType.SEMICOLON);
    }

    // if_stmt -> 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else' block_stmt)?
    private void analyseIf_stmt(int level) throws CompileError {
        expect(TokenType.IF_KW);
        analyseExpr();
        int br_false_pos,br_pos;
        br_false_pos = instructions.size();
        AddIns(Operation.BR_FALSE);
        analyseBlock_stmt(level+1);
        br_pos = instructions.size();
        if(nextIf(TokenType.ELSE_KW) != null){
            AddIns(Operation.BR);
            instructions.get(br_false_pos).setX(br_pos - br_false_pos);
            if(check(TokenType.IF_KW)){
                analyseIf_stmt(level);
            }else{
                analyseBlock_stmt(level+1);
            }
            instructions.get(br_pos).setX(instructions.size() - br_pos-1);
        }else{
            instructions.get(br_false_pos).setX(br_pos - br_false_pos-1);
        }
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
        int start = instructions.size();
        analyseExpr();
        int br_pos = instructions.size();
        AddIns(Operation.BR_FALSE);
        analyseBlock_stmt(level+1);
        AddIns(Operation.BR,(start - instructions.size() - 1));
        instructions.get(br_pos).setX(instructions.size() - br_pos-1);
    }

    // return_stmt -> 'return' expr? ';'
    private void analyseReturn_stmt(int level) throws CompileError{
        expect(TokenType.RETURN_KW);
        switch (curFunc.retType){
            case VOID:
                break;
            case INT:
            case DOUBLE:
                if(peek().getValue() == TokenType.SEMICOLON){
                    throw new AnalyzeError(ErrorCode.ErrorReturn,peekedToken.getStartPos());
                }
                AddIns(Operation.ARGA,0);
                analyseExpr();
                AddIns(Operation.STORE64);
                if(TypeStack.pop() != curFunc.retType)
                {
                    throw new AnalyzeError(ErrorCode.InvalidTypeComparion,peekedToken.getStartPos());
                }

        }
        AddIns(Operation.RET);
        expect(TokenType.SEMICOLON);
    }

    // break_stmt -> 'break' ';'
    private void analyseBreak_stmt() throws CompileError{
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
    }

    // continue_stmt -> 'continue' ';'
    private void analyseContinue_stmt() throws CompileError{
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
    }

    /*
    expr ->
    operator_expr
    | negate_expr
    | assign_expr
    | as_expr
    | call_expr
    | literal_expr
    | ident_expr
    | group_expr
     */

    private void analyseExpr() throws CompileError
    {
        if(!isExprBegin()){
            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
        }
        analyseExpr_CMP();
    }

    private void analyseExpr_CMP() throws CompileError
    {
        analyseExpr_PluMin();
        while(isCmpOp()){
            TokenType curType = next().getTokenType();
            analyseExpr_PluMin();
            AddCmpIns();
            switch (curType){
                case EQ:
                    AddIns(Operation.NOT);
                    break;
                case NEQ:
                    AddIns(Operation.NOT);
                    AddIns(Operation.NOT);
                    break;
                case LE:
                    AddIns(Operation.SET_GT);
                    AddIns(Operation.NOT);
                    break;
                case LT:
                    AddIns(Operation.SET_LT);
                    break;
                case GE:
                    AddIns(Operation.SET_LT);
                    AddIns(Operation.NOT);
                    break;
                case GT:
                    AddIns(Operation.SET_GT);
                    break;
            }
        }
    }

    private void analyseExpr_PluMin() throws CompileError
    {
        analyseExpr_MulDiv();
        while (isPorM())
        {
            TokenType curType = next().getTokenType();
            analyseExpr_MulDiv();
            SymbolType lType = TypeStack.pop();
            SymbolType rType = TypeStack.pop();
            if(lType != rType){
                throw new AnalyzeError(ErrorCode.InvalidTypeComparion,peek().getStartPos());
            }
            TypeStack.push(lType);
            if(curType == TokenType.PLUS){
                AddIns(Operation.ADD_I);
            }
            else{
                AddIns(Operation.SUB_I);
            }
        }

    }

    private void analyseExpr_MulDiv() throws CompileError
    {
        analyseExpr_As();
        while (isMorD())
        {
            TokenType curType = next().getTokenType();
            analyseExpr_As();
            SymbolType lType = TypeStack.pop();
            SymbolType rType = TypeStack.pop();
            if(lType != rType){
                throw new AnalyzeError(ErrorCode.InvalidTypeComparion,peek().getStartPos());
            }
            TypeStack.push(lType);
            if(curType == TokenType.MUL){
                AddIns(Operation.MUL_I);
            }
            else{
                AddIns(Operation.DIV_I);
            }
        }

    }

    private void analyseExpr_As() throws CompileError
    {
        analyseExpr_Neg();
    }

    private void analyseExpr_Neg() throws CompileError
    {
        int NegTimes = 0;
        while(peek().getTokenType() == TokenType.MINUS)
        {
            NegTimes++;
            next();
        }
        if(NegTimes % 2 != 0){
            AddIns(Operation.PUSH,0);
        }
        analyseExpr_Item();
        if(NegTimes % 2 != 0){
            AddIns(Operation.SUB_I);
        }
    }

    private void analyseExpr_Item() throws CompileError
    {
        if(nextIf(TokenType.L_PAREN) != null)
        {
            analyseExpr();
            expect(TokenType.R_PAREN);
        }
        Token cur = peek();
        switch(cur.getTokenType()){
            case UINT_LITERAL:
                pushUINT(cur);
                break;
            case DOUBLE_LITERAL:
                pushDOUBLE(cur);
                break;
            case STRING_LITERAL:
                pushSTRING(cur);
                break;
            case IDENT:
                pushIDENT(cur);
                break;
//            default:
//                throw new AnalyzeError(ErrorCode.InvalidType,cur.getStartPos());
        }

    }

    private void pushUINT(Token token) throws CompileError {
        expect(TokenType.UINT_LITERAL);
        AddIns(Operation.PUSH,(int)token.getValue());
        TypeStack.push(SymbolType.INT);
    }

    private void pushDOUBLE(Token token) throws CompileError {
        expect(TokenType.DOUBLE_LITERAL);
        AddIns(Operation.PUSH,Double.doubleToRawLongBits((double)token.getValue()));
        TypeStack.push(SymbolType.DOUBLE);
    }

    private void pushSTRING(Token token) throws CompileError {
        expect(TokenType.STRING_LITERAL);
        String value = (String)token.getValue();
        for(int i=value.length()-1;i>=0;i--){
            AddIns(Operation.PUSH, (int) value.charAt(i));
        }
    }

    private void pushIDENT(Token token) throws CompileError {
        expect(TokenType.IDENT);
        String name = (String) token.getValue();
        SymbolEntry symbol = symboler.findCloestSymbol(name);
        if(symbol == null){
            if(isStandard(name)){
                handleStandard(name);
                return;
            }
            else {
                throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
            }
        }
        if(symbol.type == SymbolType.FUNC_NAME){
            pushFUNC(token);
        }
        else{
            if(nextIf(TokenType.ASSIGN) != null) {
                if(symbol.isConstant){
                    throw new AnalyzeError(ErrorCode.AssignToConstant,token.getStartPos());
                }
                pushVAR(token,false);
                analyseExpr();
                AddIns(Operation.STORE64);
            }else {
                pushVAR(token,true);
            }
        }
    }

    private void pushVAR(Token token,boolean reLoad) throws CompileError {
        SymbolEntry symbol = symboler.findCloestSymbol((String) token.getValue());
        if(symbol == null){
            throw new AnalyzeError(ErrorCode.NotDeclared,token.getStartPos());
        }
        if (symbol.isGlobal) {
            AddIns(Operation.GLOBA, symbol.stackOffset);
        } else if (symbol.isParam) {
            AddIns(Operation.ARGA, symbol.stackOffset);
        } else {
            AddIns(Operation.LOCA, symbol.stackOffset);
        }
        if(reLoad){
            AddIns(Operation.LOAD64);
            if(symbol.type == SymbolType.INT){
                TypeStack.push(SymbolType.INT);
            }
            else if(symbol.type == SymbolType.DOUBLE){
                TypeStack.push(SymbolType.DOUBLE);
            }
        }
    }

    private void pushFUNC(Token token) throws CompileError
    {
        String name = token.getValueString();
        FunctionDef aimFunc = file.findFunc(name);
        if(aimFunc == null){
            throw new AnalyzeError(ErrorCode.NotDeclared,token.getStartPos());
        }

        //返回空间
        for(int i=0;i<aimFunc.ret;i++){
            AddIns(Operation.PUSH,0);
        }

        expect(TokenType.L_PAREN);
        for(int i=0;i<aimFunc.params-1;i++){
            analyseExpr();
            expect(TokenType.COMMA);
            //录入值与参数不匹配
            if(!TypeStack.peek().equals(aimFunc.paramList.get(i).type))
            {
                throw new AnalyzeError(ErrorCode.InvalidParam,peekedToken.getStartPos());
            }
        }
        analyseExpr();
        //录入值与参数不匹配
        if(!TypeStack.peek().equals(aimFunc.paramList.get(aimFunc.params-1).type))
        {
            throw new AnalyzeError(ErrorCode.InvalidParam,peekedToken.getStartPos());
        }
        expect(TokenType.R_PAREN);
        AddIns(Operation.CALL,aimFunc.name);

        if(aimFunc.retType == SymbolType.INT){
            TypeStack.push(SymbolType.INT);
        }
        else if(aimFunc.retType == SymbolType.DOUBLE){
            TypeStack.push(SymbolType.DOUBLE);
        }

    }

    private boolean isStandard(String name)
    {
        String Stds[] = {"getint","getdouble","getchar",
                "putint","putdouble","putchar","putstr","putln"};
        for(String stds:Stds){
            if(stds.equals(name))
                return true;
        }
        return false;
    }

    private void handleStandard(String name) throws CompileError {
        switch(name){
            case "getint" : {
                expect(TokenType.L_PAREN);
                expect(TokenType.R_PAREN);
                AddIns(Operation.SCAN_I);
                break;
            }
            case "getdouble" : {
                expect(TokenType.L_PAREN);
                expect(TokenType.R_PAREN);
                AddIns(Operation.SCAN_F);
                break;
            }
            case "getchar" : {
                expect(TokenType.L_PAREN);
                expect(TokenType.R_PAREN);
                AddIns(Operation.SCAN_C);
                break;
            }
            case "putint" : {
                expect(TokenType.L_PAREN);
                analyseExpr();
                expect(TokenType.R_PAREN);
                AddIns(Operation.PRINT_I);
                break;
            }
            case "putdouble" : {
                expect(TokenType.L_PAREN);
                analyseExpr();
                expect(TokenType.R_PAREN);
                AddIns(Operation.PRINT_F);
                break;
            }
            case "putstr" : {
                expect(TokenType.L_PAREN);
                if (peek().getTokenType() != TokenType.STRING_LITERAL) {
                    throw new AnalyzeError(ErrorCode.InvalidInput, peekedToken.getStartPos());
                }
                String str = (String) peek().getValue();
                analyseExpr_Item();
                for (int i = 0; i < str.length(); i++) {
                    AddIns(Operation.PRINT_C);
                }
                expect(TokenType.R_PAREN);
                break;
            }
            case "putchar" : {
                expect(TokenType.L_PAREN);
                analyseExpr();
                AddIns(Operation.PRINT_C);
                expect(TokenType.R_PAREN);
                break;
            }
            case "putln" : {
                expect(TokenType.L_PAREN);
                AddIns(Operation.PUSH, (int) '\n');
                AddIns(Operation.PRINT_C);
                expect(TokenType.R_PAREN);
                break;
            }
        }
    }

    private boolean isExprBegin() throws CompileError{
        TokenType type = peek().getTokenType();
        return type == TokenType.IDENT ||
                type == TokenType.L_PAREN ||
                type == TokenType.MINUS||
                type == TokenType.CHAR_LITERAL ||
                type == TokenType.DOUBLE_LITERAL ||
                type == TokenType.UINT_LITERAL ||
                type == TokenType.STRING_LITERAL;
    }

    private boolean isCmpOp() throws CompileError{
        TokenType type = peek().getTokenType();
        return type == TokenType.EQ ||
                type == TokenType.NEQ ||
                type == TokenType.GE ||
                type == TokenType.GT ||
                type == TokenType.LE ||
                type == TokenType.LT ;
    }

    private boolean isPorM() throws CompileError{
        TokenType type = peek().getTokenType();
        return type == TokenType.PLUS ||
                type == TokenType.MINUS ;
    }

    private boolean isMorD() throws CompileError{
        TokenType type = peek().getTokenType();
        return type == TokenType.MUL ||
                type == TokenType.DIV ;
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

    private void AddIns(Operation op)
    {
        instructions.add(new Instruction(op));
    }

    private void AddIns(Operation op,long x)
    {
        instructions.add(new Instruction(op,x));
    }

    private void AddCmpIns() throws CompileError
    {
        SymbolType lType = TypeStack.pop();
        SymbolType rType = TypeStack.pop();
        if(lType != rType){
            throw new AnalyzeError(ErrorCode.InvalidTypeComparion,peek().getStartPos());
        }
        AddIns(Operation.CMP_I);
    }

}
