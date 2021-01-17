package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.program.Functiondef;
import miniplc0java.program.Globaldef;
import miniplc0java.program.Program;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    Symboler symboler = new Symboler();

    /** 程序 */
    public Program program = new Program();

    /** 当前正在分析的函数 */
    Functiondef nowFunc = null;

    /** break 和 continue */
    List<Integer> break_levels = new ArrayList<>();
    List<Integer> break_poses = new ArrayList<>();
    List<Integer> continue_levels = new ArrayList<>();
    List<Integer> continue_poses = new ArrayList<>();
    int while_level = 0;

    /** 当前是int还是double */
    Stack<ExperType> experTypeStack = new Stack<>();

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        System.out.println("\n"+symboler);

        System.out.println(program);
//        System.out.println(program.toByteString());
        System.out.println(Arrays.toString(program.toBytes()));
        return instructions;
    }

    /**
     * 查看下一个 Token
     *
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
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     *
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
        // 先给_start占个坑
        symboler.addSymbol("_start",SymbolType.FUN_NAME,
                false,false,0,true,false,null);
        while (!check(TokenType.EOF)){
            if(check(TokenType.LET_KW)){
                analyseDeclLetStmt(0);
            }else if(check(TokenType.CONST_KW)){
                analyseDeclConstStmt(0);
            }else if(check(TokenType.FN_KW)){
                anslyseFn();
            }else{
                break;
            }
        }
        addStartFun();
        addGloName();
        addFunName();
        expect(TokenType.EOF);
    }

    private void addGloName(){
        for(SymbolEntry symbol:symboler.symbolTable){
            if(symbol.isGlobal && symbol.type != SymbolType.FUN_NAME){
                program.addGlobal(new Globaldef(symbol.isConstant,"00000000"));
            }
        }
    }

    private void addFunName(){
        for(Functiondef fun:program.functiondefList){
            fun.name_id = program.globaldefList.size();
            program.addGlobal(new Globaldef(true,fun.name));
        }
    }

    private void addMain()throws AnalyzeError{
        Functiondef mainFun = program.find("main");
        if(mainFun == null){
            throw new AnalyzeError(ErrorCode.NoMain,null);
        }
        newIns(Operation.CALL,mainFun.id);
    }
    private void addStartFun() throws AnalyzeError {

        addMain();
        Functiondef functiondef = new Functiondef();
        functiondef.name = "_start";
        functiondef.id = 0;
        functiondef.returnSize = 0;
        functiondef.params = new ArrayList<>();
        functiondef.localSize = 0;
        functiondef.instructions.addAll(instructions);
        functiondef.bodySize = instructions.size(); //(length+7)/8;
        while(instructions.size() > 0){
            instructions.remove(0);
        }
        program.functiondefList.add(0,functiondef);
    }

    private void analyseDeclStmt(int level,boolean isConst) throws CompileError{
        if(isConst)
            expect(TokenType.CONST_KW);
        else
            expect(TokenType.LET_KW);
        Token ident = expect(TokenType.IDENT);
        String name = (String)ident.getValue();
        expect(TokenType.COLON);
        Token type = expect(TokenType.IDENT);
        // 符号表相关
        SymbolType symbolType = tokenToSymbolType(type);
        if(symbolType == SymbolType.VOID_NAME){
            throw new AnalyzeError(ErrorCode.TypeError,type.getStartPos());
        }
        symboler.addSymbol(name,tokenToSymbolType(type),isConst,false,level,
                level==0,false,ident.getStartPos());
        if(isConst && peek().getTokenType() != TokenType.ASSIGN){
            throw new AnalyzeError(ErrorCode.InvalidInput,ident.getStartPos());
        }
        if(nextIf(TokenType.ASSIGN) != null){
            pushVar(ident,false);
            analyseExpr();
            newIns(Operation.STORE64);

            ExperType experType = experTypeStack.pop();
            if(symbolType == SymbolType.INT_NAME && experType != ExperType.INT ||
                    symbolType == SymbolType.DOUBLE_NAME && experType != ExperType.DOUBLE){
                throw new AnalyzeError(ErrorCode.TypeError,ident.getStartPos());
            }
        }
        if(level > 0)nowFunc.localSize++;
        expect(TokenType.SEMICOLON);
    }

    private void analyseDeclLetStmt(int level) throws CompileError{
        analyseDeclStmt(level,false);
    }

    private void analyseDeclConstStmt(int level) throws CompileError{
        analyseDeclStmt(level,true);
    }

    private void anslyseFn() throws CompileError{
        Pos startPos;
        // fn关键字
        startPos = expect(TokenType.FN_KW).getStartPos();

        // 获取函数名字
        Functiondef funEntry = new Functiondef();
        nowFunc = funEntry;
        nowFunc.localSize = 0;
        funEntry.name = (String)expect(TokenType.IDENT).getValue();
        expect(TokenType.L_PAREN);

        int beforeParamSize = symboler.symbolTable.size();

        // 获取函数参数列表
        while (!check(TokenType.R_PAREN)){
            boolean isConst = false;
            if(nextIf(TokenType.CONST_KW) != null){
                isConst = true;
            }
            Token param = expect(TokenType.IDENT);
            String paramName = (String)param.getValue();
            expect(TokenType.COLON);
            Token paramType = expect(TokenType.IDENT);
            SymbolEntry symbol = new SymbolEntry(paramName,tokenToSymbolType(paramType),
                    isConst,true,1,false,true,funEntry.params.size());
            funEntry.params.add(symbol);
            symboler.addSymbol(symbol);
            if(peek().getTokenType() != TokenType.COMMA){
                break;
            }
            expect(TokenType.COMMA);
        }
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        Token returnType = expect(TokenType.IDENT);
        SymbolType symbolType = tokenToSymbolType(returnType);
        switch (symbolType) {
            case VOID_NAME -> {
                funEntry.returnSize = 0;
                funEntry.returnType = ExperType.VOID;
            }
            case DOUBLE_NAME -> {
                funEntry.returnSize = 1;
                funEntry.returnType = ExperType.DOUBLE;
            }
            case INT_NAME -> {
                funEntry.returnSize = 1;
                funEntry.returnType = ExperType.INT;
            }
            default -> throw new AnalyzeError(ErrorCode.UnknowError, returnType.getStartPos());
        }

        // 如果函数有返回值，需要把所有参数+1

        if(funEntry.returnSize == 1){
            for(int i=beforeParamSize;i<symboler.symbolTable.size();i++){
                symboler.symbolTable.get(i).stackOffset++;
            }
        }

        // 获取函数的总长度
        int oriSize = instructions.size();

        // 解决递归问题，提前加入符号表和函数列表

        SymbolEntry symbol = symboler.addSymbol(funEntry.name,SymbolType.FUN_NAME,
                false,false,0,true,false,startPos);

        funEntry.id = symbol.stackOffset;
        program.addFunc(funEntry);

        funEntry.canReturn = false;

        // 分析函数主体
        analyseBlockStmt(0,true);

        // 检查返回路径
        if(!funEntry.canReturn){
            if(funEntry.returnSize == 1){
                throw new AnalyzeError(ErrorCode.ReturnError,startPos);
            }else if(funEntry.returnSize == 0){
                // 最后加上return
                newIns(Operation.RET);
            }
        }

        // 获取函数中新增的指令数量
        for(int i=oriSize;i<instructions.size();i++){
            funEntry.instructions.add(instructions.get(i));
        }
        funEntry.bodySize = instructions.size() - oriSize; //(length+7)/8;
        while(instructions.size() > oriSize){
            instructions.remove(oriSize);
        }

        // 获取函数中需要的局部变量数量减掉自己占用的空间
//        funEntry.localSize = symboler.symbolTable.size() - oriSymSize-1;

        // 建立函数的符号表

        //symboler.popAllLevel(0);
        if(break_levels.size() > 0){
            throw new AnalyzeError(ErrorCode.BreakError,startPos);
        }
        if(continue_levels.size() >0){
            throw new AnalyzeError(ErrorCode.ContinueError,startPos);
        }
    }

    /*
    stmt ->
      expr_stmt
    | decl_stmt let const
    | if_stmt if
    | while_stmt while
    | break_stmt break
    | continue_stmt continue
    | return_stmt return
    | block_stmt {
    | empty_stmt ;

     */
    private void analyseBlockStmt(int level,boolean canExec) throws CompileError{
        level ++;
        expect(TokenType.L_BRACE);
        while(true){
            Token peek = peek();
            TokenType type = peek.getTokenType();
            if (type == TokenType.CONST_KW) {
                analyseDeclConstStmt(level);
            } else if (type == TokenType.LET_KW) {
                analyseDeclLetStmt(level);
            } else if (type == TokenType.IF_KW) {
                analyseIfStmt(level,canExec);
            } else if(type == TokenType.WHILE_KW){
                analyseWhile(level);
            } else if(type == TokenType.RETURN_KW){
                analyseReturn();
                if(canExec){
                    nowFunc.canReturn = true;
                }
            } else if(type == TokenType.L_BRACE){
                analyseBlockStmt(level,canExec);
            } else if(type == TokenType.SEMICOLON){

                next();
            } else if(type == TokenType.R_BRACE) {
                break;
            } else if(type == TokenType.BREAK_KW) {
                analyseBreak();
            } else if(type == TokenType.CONTINUE_KW) {
                analyseContinue();
            }else{
                analyseExpr();
            }
        }
        expect(TokenType.R_BRACE);

        // 作用域清空
        symboler.popAllLevel(level-1);

    }


    private void analyseIfStmt(int level,boolean canExec) throws CompileError{
        expect(TokenType.IF_KW);
        analyseExpr();
        int br_false_pos,br_pos;
        br_false_pos = instructions.size();
        newIns(Operation.BR_FALSE);
        analyseBlockStmt(level,false);// if后面的肯定不能保证执行
        br_pos = instructions.size();
        if(nextIf(TokenType.ELSE_KW) != null){
            newIns(Operation.BR);
            instructions.get(br_false_pos).setX(br_pos - br_false_pos);
            if(check(TokenType.IF_KW)){ // else if
                analyseIfStmt(level,canExec); // else if 后面也不一定执行
            }else{
                analyseBlockStmt(level,canExec); // else
            }
            instructions.get(br_pos).setX(instructions.size() - br_pos-1);
        }else{
            instructions.get(br_false_pos).setX(br_pos - br_false_pos-1);
        }
    }

    private void analyseWhile(int level) throws CompileError{
        while_level++;
        expect(TokenType.WHILE_KW);
        int start = instructions.size();
        analyseExpr();
        int br_pos = instructions.size();
        newIns(Operation.BR_FALSE);
        analyseBlockStmt(level,false);
        newIns(Operation.BR,(start - instructions.size() - 1));

        instructions.get(br_pos).setX(instructions.size() - br_pos-1);
        System.out.println("while");
        //break
        for(int i=0;i<break_levels.size();i++){
            System.out.println(break_levels.get(i)+" "+break_poses.get(i));
            if(break_levels.get(i) == while_level){
                instructions.get(break_poses.get(i)).setX(instructions.size()-break_poses.get(i)-1);
                break_levels.remove(i);
                break_poses.remove(i);
                i--;
            }
        }

        //continue
        for(int i=0;i<continue_levels.size();i++){
            if(continue_levels.get(i) == while_level){
                System.out.println("continue "+level +" "+ continue_levels.get(i) );
                instructions.get(continue_poses.get(i)).setX((start - continue_poses.get(i) - 1));
                continue_levels.remove(i);
                continue_poses.remove(i);
                i--;
            }
        }
        while_level--;
    }

    private void analyseBreak()throws CompileError{
        expect(TokenType.BREAK_KW);
        break_levels.add(while_level);
        break_poses.add(instructions.size());
        newIns(Operation.BR,0);
    }
    private void analyseContinue()throws CompileError{
        expect(TokenType.CONTINUE_KW);
        continue_levels.add(while_level);
        continue_poses.add(instructions.size());
        newIns(Operation.BR,0);
    }
    private void analyseReturn() throws CompileError{
        expect(TokenType.RETURN_KW);
        if(nowFunc.returnType == ExperType.VOID){
            expect(TokenType.SEMICOLON);
        }else {
            if(peek().getValue() == TokenType.SEMICOLON){
                throw new AnalyzeError(ErrorCode.ReturnError,peek().getStartPos());
            }
            newIns(Operation.ARGA,0);
            analyseExpr();
            newIns(Operation.STORE64);

            if(nowFunc.returnType == ExperType.INT && experTypeStack.peek() != ExperType.INT ||
                    nowFunc.returnType == ExperType.DOUBLE && experTypeStack.peek() != ExperType.DOUBLE ){
                throw new AnalyzeError(ErrorCode.ReturnError,peek().getStartPos());
            }
        }

        newIns(Operation.RET);
    }

    private void analyseExpr() throws CompileError{
        if(!isExprBegin(peek().getTokenType())){
            throw new AnalyzeError(ErrorCode.InvalidInput,peekedToken.getStartPos());
        }
        analyseExprCmp();

        check(TokenType.SEMICOLON);
    }

    private boolean isExprBegin(TokenType type){
        return type == TokenType.IDENT ||
                type == TokenType.L_PAREN ||
                type == TokenType.MINUS||
                type == TokenType.CHAR_LITERAL ||
                type == TokenType.DOUBLE_LITERAL ||
                type == TokenType.UINT_LITERAL ||
                type == TokenType.STRING_LITERAL;
    }


    private void analyseExprCmp() throws CompileError{
        analyseExprPM();
        while (isCmpOp(peek().getTokenType())){
            TokenType type = peek().getTokenType();
            next();
            analyseExprPM();
            switch (type) {
                case GT -> {
                    newInsCmpIF();
                    newIns(Operation.SET_GT);
                }
                case LT -> {
                    newInsCmpIF();
                    newIns(Operation.SET_LT);
                }
                case GE -> {
                    newInsCmpIF();
                    newIns(Operation.SET_LT);
                    newIns(Operation.NOT);
                }
                case LE -> {
                    newInsCmpIF();
                    newIns(Operation.SET_GT);
                    newIns(Operation.NOT);
                }
                case EQ -> {
                    newInsCmpIF();
                    newIns(Operation.NOT);
                }
                case NEQ -> {
                    newInsCmpIF();
                    newIns(Operation.NOT);
                    newIns(Operation.NOT);
                }
            }
        }
    }

    private ExperType getNowExperType() throws TokenizeError, AnalyzeError {
        ExperType first = experTypeStack.pop();
        ExperType second = experTypeStack.pop();
        if(first != second){
            throw new AnalyzeError(ErrorCode.TypeError,peek().getStartPos());
        }
        return first;
    }

    private void newInsCmpIF() throws TokenizeError, AnalyzeError {
        ExperType nowType = getNowExperType();
        if(nowType == ExperType.INT)
            newIns(Operation.CMP_I);
        else
            newIns(Operation.CMP_F);
    }

    private void analyseExprPM() throws CompileError{
        analyseExprMD();
        TokenType type = peek().getTokenType();
        while (type == TokenType.PLUS || type == TokenType.MINUS){
            Token op = next();
            analyseExprMD();

            ExperType nowExperType = getNowExperType();

            if(nowExperType == ExperType.INT){
                experTypeStack.push(ExperType.INT);
            }else{
                experTypeStack.push(ExperType.DOUBLE);
            }

            if(op.getTokenType() == TokenType.PLUS) {
                if(nowExperType == ExperType.INT)
                    newIns(Operation.ADD_I);
                else
                    newIns(Operation.ADD_F);
            }else{
                if(nowExperType == ExperType.INT)
                    newIns(Operation.SUB_I);
                else
                    newIns(Operation.SUB_F);
            }

            type = peek().getTokenType();
        }
    }

    private void analyseExprMD() throws CompileError{
        analyseExprAS();
        TokenType type = peek().getTokenType();
        while (type == TokenType.MUL || type == TokenType.DIV){
            next();
            analyseExprAS();

            ExperType nowExperType = getNowExperType();

            if(nowExperType == ExperType.INT){
                experTypeStack.push(ExperType.INT);
            }else{
                experTypeStack.push(ExperType.DOUBLE);
            }

            if(type == TokenType.MUL){
                if(nowExperType == ExperType.INT)
                    newIns(Operation.MUL_I);
                else
                    newIns(Operation.MUL_F);
            }else{
                if(nowExperType == ExperType.INT)
                    newIns(Operation.DIV_I);
                else
                    newIns(Operation.DIV_F);
            }
            type = peek().getTokenType();
        }
    }

    private void analyseExprAS() throws CompileError{
        analyseExprSign();
        while (nextIf(TokenType.AS_KW) != null){
            String type = next().getValueString();
            if(type.equals("int")){
                if(experTypeStack.peek() == ExperType.DOUBLE){ // double to int
                    newIns(Operation.FTOI);
                    experTypeStack.pop();
                    experTypeStack.push(ExperType.INT);
                }
                // int to int忽略
            }
            if(type.equals("double")){
                if(experTypeStack.peek() == ExperType.INT){ // int to double
                    newIns(Operation.ITOF);
                    experTypeStack.pop();
                    experTypeStack.push(ExperType.DOUBLE);
                }
            }
            analyseExprSign();
        }
    }

    private void analyseExprSign() throws CompileError{
        TokenType type = peek().getTokenType();
        int minusCnt = 0;
        while(type == TokenType.MINUS){
            next();
            minusCnt++;
            newIns(Operation.PUSH,0);
            type = peek().getTokenType();
        }
        analyseExprItem();
        while ((minusCnt--) !=0){
            if(experTypeStack.peek() == ExperType.INT)
                newIns(Operation.SUB_I);
            else
                newIns(Operation.SUB_F);
        }
    }

    private void analyseExprItem() throws CompileError{
        if(nextIf(TokenType.L_PAREN) != null){
            analyseExpr();
            expect(TokenType.R_PAREN);
        }
        Token token = peek();
        if(token.getTokenType() == TokenType.UINT_LITERAL){
            pushUint(token);
        }else if(token.getTokenType() == TokenType.DOUBLE_LITERAL){
            pushDouble(token);
        }else if(token.getTokenType() == TokenType.IDENT){
            pushIdent(token);
        }else if(token.getTokenType() == TokenType.STRING_LITERAL){
            pushString(token);
        }
    }

    private void pushString(Token token) throws CompileError {
        expect(TokenType.STRING_LITERAL);
        String value = (String)token.getValue();
        for(int i=value.length()-1;i>=0;i--){
            newIns(Operation.PUSH, (int) value.charAt(i));
        }
    }
    private void pushUint(Token token) throws CompileError {
        expect(TokenType.UINT_LITERAL);
        newIns(Long.parseLong((String)token.getValue()));
        experTypeStack.push(ExperType.INT);
    }
    private void pushDouble(Token token) throws CompileError {
        expect(TokenType.DOUBLE_LITERAL);
        double dou = Double.parseDouble((String)token.getValue());
        newIns(Double.doubleToRawLongBits(dou));
        experTypeStack.push(ExperType.DOUBLE);
    }
    private void pushFun(Token token) throws CompileError{
        String name = (String)token.getValue();
        Functiondef function = program.find(name);
        if(function == null){
            System.out.println("Err");
            if(isStd(name)){
                pushStd(name);
            }
            throw new AnalyzeError(ErrorCode.NotDeclared,token.getStartPos());
        }

        // 申请返回空间
        for(int i=0;i<function.returnSize;i++){
            newIns(Operation.PUSH,0);
        }

        expect(TokenType.IDENT);
        // 传递参数
        expect(TokenType.L_PAREN);
        for(int i=0;i<function.params.size();i++){
            if(i != 0){
                expect(TokenType.COMMA);
            }
            analyseExpr();
        }
        // call
        newIns(Operation.CALL,function.id);

        if(function.returnType == ExperType.INT){
            experTypeStack.push(ExperType.INT);
        }else if(function.returnType == ExperType.DOUBLE){
            experTypeStack.push(ExperType.DOUBLE);
        }

        expect(TokenType.R_PAREN);
    }
    private void pushIdent(Token token) throws CompileError {
        String name = (String) token.getValue();
        SymbolEntry symbol = symboler.findSymbol(name);
        if(symbol == null){
            if(isStd(name)){
                pushStd(name);
                return;
            }else {
                throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
            }
        }
        if(symbol.type == SymbolType.FUN_NAME){
            pushFun(token);
        }else{
            expect(TokenType.IDENT);
            if(nextIf(TokenType.ASSIGN) != null) {
                if(symbol.isConstant){
                    throw new AnalyzeError(ErrorCode.AssignToConstant,token.getStartPos());
                }
                pushVar(token,false);
                analyseExpr();
                newIns(Operation.STORE64);
            }else {
                pushVar(token,true);
            }
        }
    }

    // 变量，不是函数的变量
    private void pushVar(Token token,boolean needToLoad) throws CompileError {
        SymbolEntry symbol = symboler.findSymbol((String) token.getValue());
        if(symbol == null){
            throw new AnalyzeError(ErrorCode.NoError,token.getStartPos());
        }
        if (symbol.isGlobal) {
            newIns(Operation.GLOBA, symbol.stackOffset);
        } else if (symbol.isParam) {
            newIns(Operation.ARGA, symbol.stackOffset);
        } else {
            newIns(Operation.LOCA, symbol.stackOffset);
        }
        if(needToLoad){
            newIns(Operation.LOAD64);
            if(symbol.type == SymbolType.INT_NAME){
                experTypeStack.push(ExperType.INT);
            }else{
                experTypeStack.push(ExperType.DOUBLE);
            }
        }
    }

    private boolean isCmpOp(TokenType type){
        return type == TokenType.LE || type == TokenType.GE ||
                type == TokenType.GT || type == TokenType.LT ||
                type == TokenType.EQ || type == TokenType.NEQ;
    }

    private SymbolType tokenToSymbolType(Token token) throws CompileError{
        String tokenName = (String)token.getValue();
        switch (tokenName) {
            case "int":
                return SymbolType.INT_NAME;
            case "double":
                return SymbolType.DOUBLE_NAME;
            case "void":
                return SymbolType.VOID_NAME;
        }
        throw new AnalyzeError(ErrorCode.InvalidInput,token.getStartPos());
    }

    private boolean isStd(String name){
        String[] stds = new String[]{"getint","getdouble","getchar",
                "putint","putdouble","putchar","putstr","putln"};
        for(String std:stds){
            if(name.equals(std))
                return true;
        }
        return false;
    }
    private void pushStd(String name) throws CompileError {
        expect(TokenType.IDENT);
        switch (name) {
            case "getint" -> {
                expect(TokenType.L_PAREN);
                expect(TokenType.R_PAREN);
                newIns(Operation.SCAN_I);
            }
            case "getdouble" -> {
                expect(TokenType.L_PAREN);
                expect(TokenType.R_PAREN);
                newIns(Operation.SCAN_F);
            }
            case "getchar" -> {
                expect(TokenType.L_PAREN);
                newIns(Operation.SCAN_C);
                expect(TokenType.R_PAREN);
            }
            case "putint" -> {
                expect(TokenType.L_PAREN);
                analyseExpr();
                expect(TokenType.R_PAREN);
                newIns(Operation.PRINT_I);
            }
            case "putdouble" -> {
                expect(TokenType.L_PAREN);
                analyseExpr();
                expect(TokenType.R_PAREN);
                newIns(Operation.PRINT_F);
            }
            case "putstr" -> {
                expect(TokenType.L_PAREN);
                if (peek().getTokenType() != TokenType.STRING_LITERAL) {
                    throw new AnalyzeError(ErrorCode.InvalidInput, peekedToken.getStartPos());
                }
                String str = (String) peek().getValue();
                analyseExprItem();
                for (int i = 0; i < str.length(); i++) {
                    newIns(Operation.PRINT_C);
                }
                expect(TokenType.R_PAREN);
            }
            case "putchar" -> {
                expect(TokenType.L_PAREN);
                analyseExpr();
                newIns(Operation.PRINT_C);
                expect(TokenType.R_PAREN);
            }
            case "putln" -> {
                expect(TokenType.L_PAREN);
                newIns(Operation.PUSH, (int) '\n');
                newIns(Operation.PRINT_C);
                expect(TokenType.R_PAREN);
            }
        }
    }
    
    private void newIns(Operation opt, Integer x){
        instructions.add(new Instruction(opt,(long)x));
    }
    private void newIns(Long x){
        instructions.add(new Instruction(Operation.PUSH, x));
    }
    private void newIns(Operation opt){
        instructions.add(new Instruction(opt));
    }
}
