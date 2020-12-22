package miniplc0java.error;

public enum ErrorCode {
    NoError, // Should be only used internally.
    InvalidInput,
    InvalidIdentifier,
    IntegerOverflow, // int32_t overflow.
    NoBegin,
    NoEnd,
    NeedIdentifier,
    ConstantNeedValue,
    NoSemicolon,
    InvalidVariableDeclaration,
    IncompleteExpression,
    NotDeclared,
    AssignToConstant,
    DuplicateDeclaration,
    NotInitialized,
    InvalidType,
    InvalidAssignment,
    InvalidPrint,
    ExpectedToken
}
