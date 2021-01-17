package miniplc0java.error;

public enum ErrorCode {
    NoError, // Should be only used internally.
    ReturnError,TypeError,
    BreakError,ContinueError,
    UnknowError,
    StreamError, EOF, InvalidInput, InvalidIdentifier, IntegerOverflow, // int32_t overflow.
    NoMain, NoEnd, NeedIdentifier, ConstantNeedValue, NoSemicolon, InvalidVariableDeclaration, IncompleteExpression,
    NotDeclared, AssignToConstant, DuplicateDeclaration, NotInitialized, InvalidAssignment, InvalidPrint, ExpectedToken
}
