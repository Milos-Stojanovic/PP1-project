package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.VariableHeightLayoutCache;

import rs.ac.bg.etf.pp1.CounterVisitor.*;
import rs.ac.bg.etf.pp1.SemanticAnalyzer.MethodHelper;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPc;
	
	public int getMainPc() {
		return mainPc;
	}
	
	public void visit(ProgName ProgramName) {
        super.visit(ProgramName);
        
        Obj method = Tab.find("chr");
        method.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(method.getLevel());
        Code.put(method.getLocalSymbols().size());
        Code.put(Code.load_n + 0);
        Code.put(Code.exit);
        Code.put(Code.return_);
        
        method = Tab.find("len");
        method.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(method.getLevel());
        Code.put(method.getLocalSymbols().size());
        Code.put(Code.load_n + 0);
        Code.put(Code.arraylength);
        Code.put(Code.exit);
        Code.put(Code.return_);
        
        method = Tab.find("ord");
        method.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(method.getLevel());
        Code.put(method.getLocalSymbols().size());
        Code.put(Code.load_n + 0);
        Code.put(Code.exit);
        Code.put(Code.return_);
    }
	
	public void visit(SingleStmtElemPrint stmt) {
		
		int num = Integer.MIN_VALUE;
		if (stmt.getOptConst() instanceof YesOptConst) {
			num = ((YesOptConst)stmt.getOptConst()).getN1();
		}
		
		int type = stmt.getExpr().struct.getKind();
		if(type == Struct.Int) {
			 if (num != Integer.MIN_VALUE) {
				 Code.loadConst(5);
				 Code.put(Code.print);
				 Code.loadConst((int)',');
				 Code.loadConst(1);
				 Code.put(Code.bprint);
				 Code.loadConst(num);
				 Code.loadConst(5);
			 }
            else Code.loadConst(5);
            Code.put(Code.print);
            return;
		}
		
		if(type == Struct.Char) {
			if (num != Integer.MIN_VALUE){
				 Code.loadConst(1);
				 Code.put(Code.bprint);
				 Code.loadConst((int)',');
				 Code.loadConst(1);
				 Code.put(Code.bprint);
				 Code.loadConst(num);
				 Code.loadConst(5);
				 Code.put(Code.print);
			 }
            else {
            	Code.loadConst(1);
            	Code.put(Code.bprint);
            }
            return;
		}
		
		if(type == Struct.Bool) {
			if (num != Integer.MIN_VALUE){
				 Code.loadConst(1);
				 Code.put(Code.bprint);
				 Code.loadConst((int)',');
				 Code.loadConst(1);
				 Code.put(Code.bprint);
				 Code.loadConst(num);
				 Code.loadConst(5);
				 Code.put(Code.print);
			 }
            else {
            	Code.loadConst(1);
            	Code.put(Code.bprint);
            }
            return;
		}
		
	}
	
	
	public void visit(FactorNum factorNum) { // const deo od Expr
		Obj con = Tab.insert(Obj.Con, "$", factorNum.struct);
		con.setLevel(1);
		System.out.println(con.getAdr());
		con.setAdr(factorNum.getN1());
		
		Code.load(con);
	}
	
	public void visit(FactorChar factorChar) { // const deo od Expr
		Obj con = Tab.insert(Obj.Con, "$", factorChar.struct);
		con.setLevel(1);
		char c = factorChar.getC1().charAt(1);
		con.setAdr((int)c);
		
		Code.load(con);
	}
	
	public void visit(FactorBool factorBool) { // const deo od Expr
		Obj con = Tab.insert(Obj.Con, "$", factorBool.struct);
		con.setLevel(1);
		con.setAdr((factorBool.getB1().equals("true"))?1:0);
		
		Code.load(con);
	}
	
	public void visit(MethodTypeName methodTypeName) {
		
		if("main".equalsIgnoreCase(methodTypeName.getMethName())) {
			mainPc = Code.pc;
		}
		methodTypeName.obj.setAdr(Code.pc);
		
		// Collect arguments and local variables
		SyntaxNode methodNode = methodTypeName.getParent();
		
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		
		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);
		
		// Generate the entry
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(fpCnt.getCount() + varCnt.getCount());
	}
	
	public void visit(MethodDecl methodDecl) {
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(MultipleFactor mf) {
		
		if (mf.getMulop() instanceof MulopMul) {
			Code.put(Code.mul);
		}
		if (mf.getMulop() instanceof MulopDiv) {
			Code.put(Code.div);
		}
		if (mf.getMulop() instanceof MulopMod) {
			Code.put(Code.rem);
		}
	}
	
	public void visit(MultipleTerm mt) {
	
		if (mt.getAddop() instanceof AddopPlus) {
			Code.put(Code.add);
		}
		else
			Code.put(Code.sub);
	}
	
	// DESIGNATOR 
	
	public void visit(Designator designator) {
		
	}
	
	public void visit(DesignatorStatement dsgStmt) {
		
		if(dsgStmt.getDsgArray() instanceof DsgArrayAE) {
			if(((DsgArrayAE)dsgStmt.getDsgArray()).getExpr().getTermArray() instanceof SingleTerm
					&& ((SingleTerm)((DsgArrayAE)dsgStmt.getDsgArray()).getExpr().getTermArray()).getTerm() instanceof SingleFactor
					&& ((FactorNoMinus)((SingleFactor)((SingleTerm)((DsgArrayAE)dsgStmt.getDsgArray()).getExpr().getTermArray()).getTerm()).getFactorUn()).getFactor() instanceof FactorNew) {
				
				Obj dsg = dsgStmt.getDesignator().obj;
				Code.put(Code.newarray);
				
				if(dsg.getType().getElemType() == Tab.charType) {
					Code.loadConst(0);
				}
				else {
					Code.loadConst(1);
				}
				Code.store(dsg);
				
			}
			else {
				Obj dsg = dsgStmt.getDesignator().obj;
				Code.store(dsg);
			}
		}
		if(dsgStmt.getDsgArray() instanceof DsgArrayActPars) { // DONE
			// za slucaj pozivanja metode
			SyntaxNode methodNode = dsgStmt.getDesignator().getParent();
			
			ActParsCounter fpCnt = new ActParsCounter();
			methodNode.traverseTopDown(fpCnt);

			MethodHelper mh = SemanticAnalyzer.findByName(dsgStmt.getDesignator().getI1());
			if (mh.numOfArgs > fpCnt.getCount()) {
				int index = 0;
				while((index+1) <= mh.numOfArgs) {
					if(mh.types.get(index).hasPredefinedValue && (index+1) > fpCnt.getCount()) {
						Code.loadConst(mh.types.get(index).predefinedValue);
					}
					
					index++;
				}
			}
			
			Code.put(Code.call); 
			Code.put2(-Code.pc + dsgStmt.getDesignator().obj.getAdr()+1);
					
			Code.put(Code.pop);
		}
		if(dsgStmt.getDsgArray() instanceof DsgArrayInc) { // DONE
			Obj dsg = dsgStmt.getDesignator().obj;
			Code.load(dsg);
			Code.loadConst(1);
			Code.put(Code.add);
			Code.store(dsg);
		}
		if(dsgStmt.getDsgArray() instanceof DsgArrayDec) { // DONE
			Obj dsg = dsgStmt.getDesignator().obj;
			Code.load(dsg);
			Code.loadConst(1);
			Code.put(Code.sub);
			Code.store(dsg);
		}
		
	}
	
	public void visit(FactorDesg dsg) {
		if(dsg.getFactorOptActPars() instanceof NoFactorOptActPars) {
			Code.load(dsg.getDesignator().obj);
		}
		else {
			// za slucaj pozivanja metode
			SyntaxNode methodNode = dsg.getDesignator().getParent();
			
			ActParsCounter fpCnt = new ActParsCounter();
			methodNode.traverseTopDown(fpCnt);
			//System.out.println("ASDASDASDA " + fpCnt.getCount());

			MethodHelper mh = SemanticAnalyzer.findByName(dsg.getDesignator().getI1());
			if (mh.numOfArgs > fpCnt.getCount()) {
				int index = 0;
				while((index+1) <= mh.numOfArgs) {
					if(mh.types.get(index).hasPredefinedValue && (index+1) > fpCnt.getCount()) {
						Code.loadConst(mh.types.get(index).predefinedValue);
					}
					
					index++;
				}
			}
			
			Code.put(Code.call); 
			Code.put2(-Code.pc + dsg.getDesignator().obj.getAdr()+1);
		
		}
	}
	
	public void visit(SingleStmtElemRead StatementRead) {
		if (StatementRead.getDesignator().obj.getType() == Tab.intType) {
	        Code.put(Code.read);
		}
		else {
        	Code.put(Code.bread);
        }
		Code.store(StatementRead.getDesignator().obj);
    }
	
	
	
}
