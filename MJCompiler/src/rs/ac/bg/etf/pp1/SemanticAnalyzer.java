package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Scope;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticAnalyzer extends VisitorAdaptor {
	
	Struct boolStruct;
	
	Struct currentStruct = null;
	Obj currentMethod = null;
	Struct currentMethodRetType = null;
	Scope targetScope = null;
	boolean returnDetected = false;
	Struct retRightType = null;
	Struct currExprStruct = null;
	
	
	Logger log = Logger.getLogger(getClass());
	public boolean errorDetected = false;
	
	public SemanticAnalyzer() {
		super();
		this.boolStruct = new Struct(Struct.Bool);
		
		Tab.insert(Obj.Type, "bool", boolStruct);
	}
	
	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	
	
	public void visit(Program prog) {
		Tab.chainLocalSymbols(prog.getProgName().obj);
		Tab.closeScope();
	}
	
	public void visit(ProgName progName) { 
		progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
		Tab.openScope();
	}
	
	public void visit(Type type) { 
		currentStruct = Tab.find(type.getTypeName()).getType();
		//System.out.println(currentStruct.getKind());
	}
	
	public void visit(VoidTypeMethod type) { 
		currentStruct = Tab.noType;
		currentMethodRetType = Tab.noType;
		//System.out.println(currentStruct.getKind());
	}
	
	public void visit(VarElem varElem) {
		Obj obj = Tab.find(varElem.getVarElem());
		
		if(obj != Tab.noObj && obj.getLevel() == 1) {
			// error
			report_error("postoji promenljiva", varElem);
		}
		else {
			// da li je niz
			boolean isArray = varElem.getOptArray() instanceof Array;
			
			if (isArray) {
				Struct myStruct = new Struct(Struct.Array, currentStruct);
				Tab.insert(Obj.Var, varElem.getVarElem(), myStruct);
			} 
			else {
				Tab.insert(Obj.Var, varElem.getVarElem(), currentStruct);
			}
			report_info("Deklarisana promenljiva "+varElem.getVarElem(), varElem);
		
		}
		
	}
	
	public void visit(VarDecl varDecl) {
		currentStruct = null;
	}
	
	public void visit(MethodTypeName methodTypeName) {
		currentMethod = Tab.insert(Obj.Meth, methodTypeName.getMethName(), currentStruct);
		currentMethodRetType = currentStruct;
		methodTypeName.obj = currentMethod;
		Tab.openScope();
		report_info("Obradjuje se funkcija "+ methodTypeName.getMethName(), methodTypeName);
	}
	
	public void visit(MethodDecl methodDecl) {
		
		if (!returnDetected && currentMethodRetType != Tab.noType) {
			report_error("Greska na liniji " + methodDecl.getLine() + ", "
					+ " metoda " + methodDecl.getMethodTypeName().getMethName() + " nema return!", null);
		}
		
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		
		currentMethod = null;
		currentMethodRetType = null;
		returnDetected = false;
		retRightType = null;
	}
	
	public void visit(Designator designator) {
		Obj obj = Tab.find(designator.getI1());
		if(obj == Tab.noObj) {
			report_error("Greska na liniji " + designator.getLine() + ": "
					+ "ime " + designator.getI1() + " nije deklarisano!", null);
		}
		designator.obj = obj;
	}
	
	public void visit(DesignatorStatement designatorStatement) {
		if(!(designatorStatement.getDsgArray() instanceof DsgArrayActPars)) {
			//report_info(factorDesignator.getFactorOptActPars().toString(), null);
			Obj obj = Tab.find(designatorStatement.getDesignator().getI1());
			//report_info(designatorStatement.getDesignator().getI1(), null);
			if(obj == Tab.noObj) {
				report_error("Greska na liniji " + designatorStatement.getDesignator().getLine() + ": "
						+ "funkcija " + designatorStatement.getDesignator().getI1() + " nije deklarisana!", null);
			}
			designatorStatement.getDesignator().obj = obj;
		}
		if(designatorStatement.getDsgArray() instanceof DsgArrayActPars) {
			//report_info(factorDesignator.getFactorOptActPars().toString(), null);
			Obj obj = Tab.find(designatorStatement.getDesignator().getI1());
			if(obj == Tab.noObj) {
				report_error("Greska na liniji " + designatorStatement.getDesignator().getLine() + ": "
						+ "ime " + designatorStatement.getDesignator().getI1() + " nije deklarisano!", null);
			}
			designatorStatement.getDesignator().obj = obj;
		}
		
	}
	
	public void visit(FormParsElem argElem) {
		Obj obj = Tab.find(argElem.getElem());
			
		if(obj != Tab.noObj && obj.getLevel() == 1) {
			// error
			report_error("postoji promenljiva", argElem);
		}
		else {
			boolean isArray = argElem.getOptArray() instanceof Array;
			
			if (isArray) {
				Struct myStruct = new Struct(Struct.Array, currentStruct);
				Tab.insert(Obj.Var, argElem.getElem(), myStruct);
			} 
			else {
				Tab.insert(Obj.Var, argElem.getElem(), currentStruct);
			}
			
			Tab.insert(Obj.Var, argElem.getElem(), currentStruct);
			report_info("Deklarisana promenljiva "+argElem.getElem(), argElem);
		}
	}
	
	public void visit(VarArgs varArg) {
		Obj obj = Tab.find(varArg.getElem());
			
		if(obj != Tab.noObj && obj.getLevel() == 1) {
			// error
			report_error("postoji promenljiva", varArg);
		}
		else {
			Struct myStruct = new Struct(Struct.Array, currentStruct);
			Tab.insert(Obj.Var, varArg.getElem(), myStruct);
			report_info("Deklarisana ... promenljiva "+varArg.getElem(), varArg);
		}
	}
	
	public void visit(Expr expr) {
		expr.struct = expr.getTermArray().struct;
	}
	
	public void visit(YesExprOpt exprOpt) {
		currExprStruct = exprOpt.getExpr().struct;
	}
	
	public void visit(SingleStmtElemReturn returnStmt) {
		returnDetected = true;
		if(currExprStruct != null) {
			retRightType = currExprStruct;
		}
		
		if(returnStmt.getExprOpt() instanceof NoExprOpt) {
			// return;
			if (currentMethodRetType != Tab.noType) {
				report_error("Greska u funkciji " + currentMethod.getName() + ": "
						+ "povratni tip funkcije nije void a treba da bude!", null);
			}
		}
		else {
			if (retRightType != currentMethodRetType) {
				report_error("Greska u funkciji " + currentMethod.getName() + ": "
						+ "povratni tip funkcije se ne poklapa sa navedenim povratnim tipom!", null);
			}
		}
		retRightType = null;
		currExprStruct = null;
	}
	
	public void visit(FactorNum factor) {
		factor.struct = Tab.intType;
	}
	
	public void visit(FactorChar factor) {
		factor.struct = Tab.charType;
	}
	
	public void visit(FactorBool factor) {
		factor.struct = boolStruct;
	}
	
	public void visit(FactorDesg factor) {
		factor.struct = factor.getDesignator().obj.getType();
	}
	
	public void visit(FactorNew factor) {
		// postavlja tip strukture u visit(Type)
	}
	
	public void visit(FactorExpr factor) {
		// postavlja tip strukture u visit(Type)
	}
	
	public void visit(SingleFactor singleFact) {
		singleFact.struct = singleFact.getFactor().struct;
	}
	
	public void visit(MultipleFactor multiFactor) {
		Struct t = multiFactor.getTerm().struct;
		//System.out.println(t);
		Struct f = multiFactor.getFactor().struct;
		//System.out.println(f);
		if(t.equals(f) && t == Tab.intType){
			multiFactor.struct = t;
    	}else{
			report_error("Greska na liniji "+ multiFactor.getLine()+" : nekompatibilni tipovi u izrazu za mnozenje.", null);
			multiFactor.struct = Tab.noType;
    	}
	}
	
	public void visit(SingleTerm singleTerm) {
		singleTerm.struct = singleTerm.getTerm().struct;
	}
	
	public void visit(MultipleTerm multiTerm) {
		Struct te = multiTerm.getTermArray().struct;
		Struct t = multiTerm.getTerm().struct;
		if(te.equals(t) && te == Tab.intType){
			multiTerm.struct = te;
    	}else{
			report_error("Greska na liniji "+ multiTerm.getLine()+" : nekompatibilni tipovi u izrazu za sabiranje.", null);
			multiTerm.struct = Tab.noType;
    	}
	}
	
	public void visit(NumConstType numConstType) {
		numConstType.struct = Tab.intType;
	}
	
	public void visit(CharConstType charConstType) {
		charConstType.struct = Tab.charType;
	}
	
	public void visit(BoolConstType boolConstType) {
		boolConstType.struct = boolStruct;
	}
	
	public void visit(ConstElem constElem) {
		if(currentStruct != constElem.getConstType().struct) {
			report_error("Greska na liniji "+ constElem.getLine()+" : nekompatibilni tipovi u definiciji konstante.", null);
			constElem.struct = Tab.noType;
		}
		else {
			report_info("Deklarisana konstanta " + constElem.getI1() + " na liniji " + constElem.getLine(), null);
			constElem.struct = currentStruct;
			Tab.insert(Obj.Con, constElem.getI1(), currentStruct);
		}
	}
	
	

}
