package org.minic.ast;

public class MemberAccessNode extends ExpressionNode {
    private final ExpressionNode struct;
    private final String memberName;
    
    public MemberAccessNode(ExpressionNode struct, String memberName) {
        this.struct = struct;
        this.memberName = memberName;
    }
    
    public ExpressionNode getStruct() {
        return struct; 
    }

    public String getMemberName() { 
        return memberName; 
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public ExpressionNode cloneNode() {
        return new MemberAccessNode(struct.cloneNode(), memberName);
    }
}