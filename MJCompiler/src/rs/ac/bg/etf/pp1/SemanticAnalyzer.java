package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

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
	int currMethodParNum = 0;
	Obj currentDesignator = null;
	Obj currentMethodDesignator = null;
	boolean optSubFlag = false;
	Obj currentLeftSideDesignator = null;
	int doWhileCnt = 0;
	boolean arrayElem = false;
	boolean leftSideArrayElem = false;
	ArrayList<TypeHelper> types = new ArrayList<>();

	boolean mainDetected = false;
	int nVars;
	
	
	public class MethodHelper{
		public String methodName;
		public int numOfArgs;
		public ArrayList<TypeHelper> types;
		public boolean lastPasIsWildcard = false;
		
		public MethodHelper(String a, int b) {
			methodName = a;
			numOfArgs = b;
			types = new ArrayList<>();
		}
		
		@Override
		public String toString() {
			return methodName + ": " + numOfArgs + ":\n" + types.toString();
		}
	}
	
	public class TypeHelper{
		public Struct type;
		public boolean hasPredefinedValue;
		public boolean wildcard;
		
		public TypeHelper(Struct s, boolean b, boolean c) {
			type = s;
			hasPredefinedValue = b;
			wildcard = c;
		}
		
		@Override
		public String toString() {
			return type.getKind() + "\n";
		}
		
	}
	
	ArrayList<MethodHelper> methods = new ArrayList<>();
	MethodHelper currentMethodHelper = null;
	int currentMethodHelperIndex = 0;
	
	public MethodHelper findByName(String name) {
		for(int i = 0; i < methods.size(); i++) {
			if(methods.get(i).methodName.equals(name))
				return methods.get(i);
		}
		return null;
	}
	
	
	Logger log = Logger.getLogger(getClass());
	public boolean errorDetected = false;

	public SemanticAnalyzer() {
		super();
		this.boolStruct = new Struct(Struct.Bool);

		Tab.insert(Obj.Type, "bool", boolStruct);
		
		currentMethodHelper = new MethodHelper("len", 1);
		currentMethodHelper.types.add(new TypeHelper(new Struct(Struct.Array, Tab.intType), false, false));
		methods.add(currentMethodHelper);
		
		currentMethodHelper = new MethodHelper("chr", 1);
		currentMethodHelper.types.add(new TypeHelper(Tab.intType, false, false));
		methods.add(currentMethodHelper);
		
		currentMethodHelper = new MethodHelper("ord", 1);
		currentMethodHelper.types.add(new TypeHelper(Tab.charType, false, false));
		methods.add(currentMethodHelper);
		
		//System.out.println(methods);
	}

	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0 : info.getLine();
		if (line != 0)
			msg.append(" na liniji ").append(line);
		log.info(msg.toString());
	}

	public void visit(Program prog) {
		nVars = Tab.currentScope().getnVars();
		Tab.chainLocalSymbols(prog.getProgName().obj);
		Tab.closeScope();
	}

	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
		Tab.openScope();
	}

	public void visit(Type type) {
		currentStruct = Tab.find(type.getTypeName()).getType();
		if (currentStruct == null) {
			report_error("Greska u liniji " + type.getLine() + ", ne postoji tip " + type.getTypeName(), null);
			currentStruct = Tab.noType;
		}
		// System.out.println(currentStruct.getKind());
	}

	public void visit(VoidTypeMethod type) {
		currentStruct = Tab.noType;
		currentMethodRetType = Tab.noType;
		// System.out.println(currentStruct.getKind());
	}

	public void visit(VarElem varElem) {
		Obj obj = Tab.find(varElem.getVarElem());

		if (obj != Tab.noObj && obj.getLevel() == 1) {
			// error
			report_error("postoji promenljiva", varElem);
		} else {
			// da li je niz
			boolean isArray = varElem.getOptArray() instanceof Array;

			if (isArray) {
				Struct myStruct = new Struct(Struct.Array, currentStruct);
				Tab.insert(Obj.Var, varElem.getVarElem(), myStruct);
			} else {
				Tab.insert(Obj.Var, varElem.getVarElem(), currentStruct);
			}
			report_info("Deklarisana promenljiva " + varElem.getVarElem(), varElem);

		}

	}

	public void visit(VarDecl varDecl) {
		currentStruct = null;
	}

	public void visit(MethodTypeName methodTypeName) {

		Obj obj = Tab.find(methodTypeName.getMethName());

		if (obj != Tab.noObj) {
			currentMethodHelper = new MethodHelper(methodTypeName.getMethName(), 0);
			//System.out.println(methodTypeName.getMethName());
			currentMethod = obj;
			currentMethodRetType = currentStruct;
			methodTypeName.obj = currentMethod;
			Tab.openScope();
			report_info("Obradjuje se funkcija " + methodTypeName.getMethName(), methodTypeName);
			report_error("postoji funkcija ", methodTypeName);
		} else {
			currentMethod = Tab.insert(Obj.Meth, methodTypeName.getMethName(), currentStruct);
			currentMethodHelper = new MethodHelper(methodTypeName.getMethName(), 0);
			currentMethodRetType = currentStruct;
			methodTypeName.obj = currentMethod;
			Tab.openScope();
			report_info("Obradjuje se funkcija " + methodTypeName.getMethName(), methodTypeName);
		}
	}

	public void visit(MethodDecl methodDecl) {

		if (!returnDetected && currentMethodRetType != Tab.noType) {
			report_error("Greska na liniji " + methodDecl.getLine() + ", " + " metoda "
					+ methodDecl.getMethodTypeName().getMethName() + " nema return!", null);
		}

		if (currentMethodRetType == Tab.noType && methodDecl.getMethodTypeName().getMethName().equals("main")
				&& currMethodParNum == 0) {
			mainDetected = true;
		}

		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();

		//System.out.println(currentMethod.getName());
		//System.out.println(currentMethodHelper.methodName);
		//currentMethodHelper.methodName = currentMethod.getName();
		//currentMethodHelper.numOfArgs = currMethodParNum;
		
		currentMethodHelper = new MethodHelper(currentMethod.getName(), currMethodParNum);
		currentMethodHelper.types = types;
		methods.add(currentMethodHelper);
		//System.out.println(methods);
		
		types = new ArrayList<>();
		currentMethod = null;
		currentMethodRetType = null;
		returnDetected = false;
		retRightType = null;
		currentMethodHelper = null;
		currMethodParNum = 0;
	}
	
	public void visit(SingleStatementElem elem) {
		leftSideArrayElem = false;
	}

	public void visit(Designator designator) {
		arrayElem = false;
		
		Obj obj = Tab.find(designator.getI1());
		if (obj == Tab.noObj) {
			report_error("Greska na liniji " + designator.getLine() + ": " + "ime " + designator.getI1()
					+ " nije deklarisano!", null);
		}
		designator.obj = obj;
		
		if (obj == Tab.noObj) {
			//currentStruct = Tab.noType;
		}
		else {
			//currentStruct = obj.getType();
		}
		currentDesignator = obj;
		if (obj.getKind() == Obj.Meth) {
			currentMethodDesignator = obj;
		}
		if(designator.getDsgOpt() instanceof DesignatorOpt && currentLeftSideDesignator != null) {
			arrayElem = true;
			if(designator.obj.getType().getElemType() == null) {
				report_error("Greska, na liniji " + designator.getLine() + " se ne nalazi promenljiva tipa niza!", null);
			}
			else {
				//currentStruct = obj.getType().getElemType();
			}
		}
		else {
			if(designator.getDsgOpt() instanceof DesignatorOpt) {
				arrayElem = true;
			}
		}
	}

	public void visit(DesignatorStatement designatorStatement) {
		if (!(designatorStatement.getDsgArray() instanceof DsgArrayActPars)) {
			// report_info(factorDesignator.getFactorOptActPars().toString(), null);
			Obj obj = Tab.find(designatorStatement.getDesignator().getI1());
			// report_info(designatorStatement.getDesignator().getI1(), null);
			if (obj == Tab.noObj) {
				report_error("Greska na liniji " + designatorStatement.getDesignator().getLine() + ": " + "funkcija "
						+ designatorStatement.getDesignator().getI1() + " nije deklarisana!", null);
			}
			designatorStatement.getDesignator().obj = obj;
		}
		if (designatorStatement.getDsgArray() instanceof DsgArrayActPars) {
			// report_info(factorDesignator.getFactorOptActPars().toString(), null);
			Obj obj = Tab.find(designatorStatement.getDesignator().getI1());
			if (obj == Tab.noObj) {
				/*
				 * report_error("Greska na liniji " +
				 * designatorStatement.getDesignator().getLine() + ": " + "ime " +
				 * designatorStatement.getDesignator().getI1() + " nije deklarisano!", null);
				 */
			}
			designatorStatement.getDesignator().obj = obj;
		}
		currentDesignator = null;
		currentMethodDesignator = null;

	}

	public void visit(FormParsElem argElem) {
		Obj obj = Tab.find(argElem.getElem());

		if (obj != Tab.noObj && obj.getLevel() == 1) {
			// error
			report_error("postoji promenljiva", argElem);
		} else {
			currMethodParNum++;
			//System.out.println(currMethodParNum);
			
			boolean isArray = argElem.getOptArray() instanceof Array;

			if (isArray) {
				Struct myStruct = new Struct(Struct.Array, currentStruct);
				types.add(currMethodParNum-1, new TypeHelper(myStruct, false, false));
				Tab.insert(Obj.Var, argElem.getElem(), myStruct);
			} else {
				types.add(currMethodParNum-1, new TypeHelper(currentStruct, false, false));
				Tab.insert(Obj.Var, argElem.getElem(), currentStruct);
			}

			// Tab.insert(Obj.Var, argElem.getElem(), currentStruct);
			report_info("Deklarisana promenljiva " + argElem.getElem(), argElem);
		}
	}

	public void visit(OptArgsElem optArg) {
		Obj obj = Tab.find(optArg.getT());

		if (obj != Tab.noObj && obj.getLevel() == 1) {
			// error
			report_error("postoji promenljiva", optArg);
		} else {
			
			if (currentStruct != optArg.getConstType().struct) {
				report_error("Greska na liniji "+optArg.getLine() +", dodeljena vrednost se ne poklapa sa tipom parametra "+(currMethodParNum+1), null);
			}
			
			currMethodParNum++;
			//System.out.println(currMethodParNum);

			Tab.insert(Obj.Var, optArg.getT(), currentStruct);
			types.add(currMethodParNum-1, new TypeHelper(currentStruct, true, false));
			report_info("Deklarisana promenljiva " + optArg.getT(), optArg);
		}
	}

	public void visit(Expr expr) {
		expr.struct = expr.getTermArray().struct;
		/*if(arrayElem) {
			expr.struct = expr.struct.getElemType();
			arrayElem = false;
		}*/
	}

	/*
	 * public void visit(ExprSingle exprSingle) { if(optSubFlag &&
	 * exprSingle.getTerm().struct != Tab.intType) {
	 * report_error("Greska na liniji "+
	 * exprSingle.getLine()+" : ne moze stajati minus ispred tipa koji nije int!",
	 * null); exprSingle.struct = Tab.noType; } else { exprSingle.struct =
	 * exprSingle.getTerm().struct; } }
	 * 
	 * public void visit(ExprMore exprMore) { Struct te = exprMore.getTerm().struct;
	 * Struct t = exprMore.getTermArray().struct; if(te.equals(t) && te ==
	 * Tab.intType){ exprMore.struct = te; }else{ if(te != Tab.intType)
	 * report_error("Greska na liniji "+
	 * exprMore.getLine()+" : ne moze stajati minus ispred tipa koji nije int!",
	 * null); report_error("Greska na liniji "+
	 * exprMore.getLine()+" : nekompatibilni tipovi u izrazu za sabiranje.", null);
	 * exprMore.struct = Tab.noType; } }
	 */

	public void visit(OptSub optSub) {
		if (optSub instanceof YesOptSub) {
			optSubFlag = true;
		}
	}

	public void visit(YesExprOpt exprOpt) {
		currExprStruct = exprOpt.getExpr().struct;
	}

	public void visit(SingleStmtElemReturn returnStmt) {
		returnDetected = true;
		if (currentMethodRetType == null) {
			report_error("Greska na liniji " + currentMethod.getName() + ", "
					+ "return je naveden van tela metode!", null);
		}
		if (currExprStruct != null) {
			retRightType = currExprStruct;
		}

		if (returnStmt.getExprOpt() instanceof NoExprOpt) {
			// return;
			if (currentMethodRetType != Tab.noType) {
				report_error("Greska u funkciji " + currentMethod.getName() + ": "
						+ "povratni tip funkcije nije void a treba da bude!", null);
			}
		} else {
			if (!retRightType.equals(currentMethodRetType)) {
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
		if(arrayElem) {
			factor.struct = factor.struct.getElemType();
			//System.out.println(factor.struct.getKind());
			arrayElem = false;
		}
		// System.out.println(factor.getDesignator().obj.getType().getKind());
		if (currentMethodDesignator != null) {
			Obj obj = Tab.find(currentMethodDesignator.getName());
			if (!(obj.getKind() == Obj.Meth) || factor.getDesignator().obj.getType().getKind() == 0) {
				report_error("Greska, na liniji " + factor.getLine() + " se ne nalazi globalna funkcija!", null);
				factor.struct = Tab.noType;
			}
		} else {
			if (factor.getDesignator().obj.getType().getKind() == 0) {
				report_error("Greska, na liniji " + factor.getLine() + " se ne nalazi globalna funkcija!", null);
				factor.struct = Tab.noType;
			}
		}
	}

	public void visit(FactorNew factor) {
		if(factor.getOptExpr().struct != Tab.intType) {
			report_error("Greska, na liniji " + factor.getLine() + " se ne nalazi tip int unutar uglastih zagrada!", null);
			factor.struct = Tab.noType;
		}
		else {
			if(factor.getOptExpr() instanceof OptionalExpr) {
				//System.out.println("ASDASD");
				factor.struct = new Struct(Struct.Array, currentStruct);
			}
			else {
				report_error("Greska na liniji " + factor.getLine() + ", sta je ovo?", null);
				factor.struct = Tab.noType;
			}
		}
	}

	public void visit(FactorExpr factor) {
		// postavlja tip strukture u visit(Type)
	}

	public void visit(FactorMinus fm) {
		fm.struct = fm.getFactor().struct;
		if (fm.struct != Tab.intType) {
			report_error("Greska na liniji " + fm.getLine() + " : ne moze stajati minus ispred tipa koji nije int!",
					null);
			fm.struct = Tab.noType;
		}
	}

	public void visit(FactorNoMinus fnm) {
		fnm.struct = fnm.getFactor().struct;
	}

	public void visit(SingleFactor singleFact) {
		singleFact.struct = singleFact.getFactorUn().struct;
	}

	public void visit(MultipleFactor multiFactor) {
		Struct t = multiFactor.getTerm().struct;
		// System.out.println(t);
		Struct f = multiFactor.getFactorUn().struct;
		// System.out.println(f);
		if (t.equals(f) && t == Tab.intType) {
			multiFactor.struct = t;
		} else {
			report_error("Greska na liniji " + multiFactor.getLine() + " : nekompatibilni tipovi u izrazu za mnozenje.",
					null);
			multiFactor.struct = Tab.noType;
		}
	}

	public void visit(Term term) {
		/*
		 * if(optSubFlag && term.struct != Tab.intType) {
		 * report_error("Greska na liniji "+
		 * term.getLine()+" : ne moze stajati minus ispred tipa koji nije int!", null);
		 * } optSubFlag = false;
		 */
	}

	public void visit(SingleTerm singleTerm) {
		singleTerm.struct = singleTerm.getTerm().struct;
	}

	public void visit(MultipleTerm multiTerm) {
		Struct te = multiTerm.getTermArray().struct;
		Struct t = multiTerm.getTerm().struct;
		//System.out.println(te.getKind()+", "+ t.getKind());
		if (te.equals(t) && te == Tab.intType) {
			multiTerm.struct = te;
		} else {
			//if()
			report_error("Greska na liniji " + multiTerm.getLine() + " : nekompatibilni tipovi u izrazu za sabiranje.",
					null);
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
		if (!currentStruct.equals(constElem.getConstType().struct)) {
			report_error("Greska na liniji " + constElem.getLine() + " : nekompatibilni tipovi u definiciji konstante.",
					null);
			constElem.struct = Tab.noType;
		} else {
			report_info("Deklarisana konstanta " + constElem.getI1() + " na liniji " + constElem.getLine(), null);
			constElem.struct = currentStruct;
			Tab.insert(Obj.Con, constElem.getI1(), currentStruct);
		}
	}

	public void visit(DsgArrayAE dsgStmt) {
		Obj obj = Tab.find(currentLeftSideDesignator.getName());
		if (!(obj.getKind() == Obj.Var)) {
			report_error("Greska, na liniji " + dsgStmt.getLine() + " se ne nalazi promenljiva/element niza!", dsgStmt);
		}
		Struct dsg = currentLeftSideDesignator.getType();
		//System.out.println(currentDesignator.getName());
		Struct assigned = dsgStmt.getExpr().struct;
		//System.out.println(leftSideArrayElem);
		if(leftSideArrayElem) {
			dsg = dsg.getElemType();
			leftSideArrayElem = false;
		}
		 //System.out.println(dsg.getKind());
		 //System.out.println(assigned.getKind());
		if (assigned.assignableTo(dsg)) {
		} else {
			report_error("Greska na liniji " + dsgStmt.getLine()
				+ " : nekompatibilni tipovi u izrazu za dodelu vrednosti.", null);
		}
		currentLeftSideDesignator = null;
	}

	public void visit(DsgArrayInc inc) {
		Obj obj = Tab.find(currentDesignator.getName());
		if (!(obj.getKind() == Obj.Var)) {
			report_error("Greska, na liniji " + inc.getLine() + " se ne nalazi promenljiva/element niza!", inc);
		}
		if (obj.getType() != Tab.intType) {
			report_error(
					"Greska na liniji " + inc.getLine() + " : promenljiva cija se vrednost inkrementira nije tipa int.",
					null);
		}
	}

	public void visit(DsgArrayDec dec) {
		Obj obj = Tab.find(currentDesignator.getName());
		if (!(obj.getKind() == Obj.Var)) {
			report_error("Greska, na liniji " + dec.getLine() + " se ne nalazi promenljiva/element niza!", dec);
		}
		if (obj.getType() != Tab.intType) {
			report_error(
					"Greska na liniji " + dec.getLine() + " : promenljiva cija se vrednost dekrementira nije tipa int.",
					null);
		}
	}

	public void visit(DsgArrayActPars dsgPars) {
		// System.out.println(currentMethodDesignator);
		currentMethodHelperIndex = 0;
		Obj obj = null;
		if (currentMethodDesignator != null)
			obj = Tab.find(currentMethodDesignator.getName());
		if ( obj == null || !(obj.getKind() == Obj.Meth)) {
			report_error("Greska, na liniji " + dsgPars.getLine() + " se ne nalazi globalna funkcija!", dsgPars);
		}
	}

	public void visit(OptionalExpr optExpt) {
		optExpt.struct = optExpt.getExpr().struct;
	}
	
	public void visit(Assignop op) {
		currentLeftSideDesignator = currentDesignator;
		if(arrayElem)
			leftSideArrayElem = true;
	}
	
	public void visit(DsgElemExpr dsgElemExpr) {
		if(dsgElemExpr.getExpr().struct != Tab.intType) {
			report_error("Greska, na liniji " + dsgElemExpr.getLine() + " se ne nalazi tip int!", dsgElemExpr);
			dsgElemExpr.struct = Tab.noType;
		}
		else {
			dsgElemExpr.struct = dsgElemExpr.getExpr().struct;
		}
	}
	
	public void visit(DesignatorOpt dsgOpt) {
		dsgOpt.struct = dsgOpt.getDsgElem().struct;
	}
	
	// do-while begin
	
	public void visit(DoWhileBegin dwb) {
		doWhileCnt++;
	}
	
	public void visit(SingleStmtElemDo doWhileStmt) {
		doWhileCnt--;
	}
	
	public void visit(SingleStmtElemBreak stmt) {
		if(doWhileCnt == 0) {
			report_error("Greska na liniji " + stmt.getLine() + ", pozvan break van do while petlje!", null);
		}
	}
	
	public void visit(SingleStmtElemContinue stmt) {
		if(doWhileCnt == 0) {
			report_error("Greska na liniji " + stmt.getLine() + ", pozvan continue van do while petlje!", null);
		}
	}
	
	// do-while end
	
	public void visit(SingleStmtElemRead stmt) {
		Obj obj = Tab.find(stmt.getDesignator().getI1());
		
		if (stmt.getDesignator().getDsgOpt() instanceof DesignatorOpt) {
			if (stmt.getDesignator().obj.getType().getElemType() == null || stmt.getDesignator().obj.getType().getElemType() == Tab.noType) {
				report_error("Greska, na liniji " + stmt.getDesignator().getLine() + " argumenti funkcije read nisu valjani!", null);
			}
		}
		else {
			if(!(obj.getType() == Tab.intType || obj.getType() == Tab.charType
					|| obj.getType() == boolStruct)) {
				report_error("Greska, na liniji " + stmt.getDesignator().getLine() + " argumenti funkcije read nisu valjani!", null);
			}
		}
	}
	
	public void visit(SingleStmtElemPrint stmt) {
		Struct s = stmt.getExpr().struct;
		//System.out.println(arrayElem);
		//System.out.println(s.getKind());
		if(!(s == Tab.intType || s == Tab.charType || s == boolStruct)) {
			report_error("Greska, na liniji " + stmt.getExpr().getLine() + " argumenti funkcije print nisu valjani!", null);
		}
	}
	
	// condition related stuff begin

	public void visit(SingleCondFact singleF) {
		singleF.struct = singleF.getExpr().struct;
	}
	
	public void visit(DoubleCondFact doubleF) {
		Struct s1 = doubleF.getExpr().struct;
		Struct s2 = doubleF.getExpr1().struct;
		if(!s1.compatibleWith(s2)) {
			report_error("Greska na liiniji " + doubleF.getLine()+", tipovi nisu kompatibilni!", null);
			doubleF.struct = Tab.noType;
		}
		else {
			if((s1.getKind() == 3 || s2.getKind() == 3)
				&& !(doubleF.getRelop() instanceof ArrayRelop1)) {
				report_error("Greska na liniiji " + doubleF.getLine()+", tipovi nisu kompatibilni!", null);
				doubleF.struct = Tab.noType;
				doubleF.struct = boolStruct;
			}
			else {
				doubleF.struct = boolStruct;
			}
		}
	}
	
	public void visit(SingleCondTermFact singleCond) {
		singleCond.struct = singleCond.getCondFact().struct;
	}
	
	public void visit(MultipleCondTermFacts multiCond) {
		Struct s1 = multiCond.getCondTerm().struct;
		Struct s2 = multiCond.getCondFact().struct;
		if(!(s1.equals(s2) && s1 == boolStruct)) {
			report_error("Greska na liiniiji " + multiCond.getLine()+", tipovi nisu kompatibilni!", null);
			multiCond.struct = Tab.noType;
		}
		else {
			multiCond.struct = boolStruct;
		}
	}
	
	public void visit(SingleCondition singleCond) {
		singleCond.struct = singleCond.getCondTerm().struct;
		if(singleCond.struct != boolStruct) {
			report_error("Greska na liiniiji " + singleCond.getLine()+", uslov mora biti tipa bool!", null);
			singleCond.struct = Tab.noType;
		}
	}
	
	public void visit(MultipleCondition multiCond) {
		Struct s1 = multiCond.getCondition().struct;
		Struct s2 = multiCond.getCondTerm().struct;
		if(!(s1.equals(s2) && s1 == boolStruct)) {
			report_error("Greska na liiniiji " + multiCond.getLine()+", tipovi nisu kompatibilni!", null);
			multiCond.struct = Tab.noType;
		}
		else {
			multiCond.struct = boolStruct;
		}
	}
	
	// condition related stuff end
	
	
	// act pars stuff begin
	
	public void visit(MoreActPars singlePar) {
		if(currentMethodDesignator != null) {
			Obj methodObj = Tab.find(currentMethodDesignator.getName());
			if(methodObj == Tab.noObj) {
				report_error("Greska tip 1 na liniji "+singlePar.getLine(), null);
			}
			currentMethodHelper = findByName(currentMethodDesignator.getName());
			//System.out.println("HMHMHM");
			if (currentMethodHelperIndex+1 <= currentMethodHelper.numOfArgs) {
				// provera kompatibilnosti tipova
				if (singlePar.getExpr().struct.compatibleWith(currentMethodHelper.types.get(currentMethodHelperIndex).type)) {
					//System.out.println("NICE!");
				}
				else {
					report_error("Greska na liniji "+singlePar.getLine()+", pogresan tip je prosledjen kao "+(currentMethodHelperIndex+1)+". argument!", null);
					}
				
				currentMethodHelperIndex++;
			}
			else {
				report_error("Greska tip 2 na liniji "+singlePar.getLine(), null);
			}
			//currentMethodHelper.types.get(currentMethodHelperIndex);
		}
	}
	
	public void visit(SingleActPars singlePar) {
		if(currentMethodDesignator != null) {
			Obj methodObj = Tab.find(currentMethodDesignator.getName());
			if(methodObj == Tab.noObj) {
				report_error("Greska tip 1 na liniji "+singlePar.getLine(), null);
			}
			//System.out.println(currentMethodDesignator.getName());
			currentMethodHelper = findByName(currentMethodDesignator.getName());
			//System.out.println("HMHMHM");
			//System.out.println(currentMethodDesignator.getName());
			//System.out.println(currentMethodHelper.numOfArgs);
			//System.out.println(currentMethodHelper.types.size());
			if (currentMethodHelperIndex+1 <= currentMethodHelper.numOfArgs) {
				// provera kompatibilnosti tipova
				if (singlePar.getExpr().struct.compatibleWith(currentMethodHelper.types.get(currentMethodHelperIndex).type)) {
					//System.out.println("NICE!");
				}
				else {
					report_error("Greska na liniji "+singlePar.getLine()+", pogresan tip je prosledjen kao "+(currentMethodHelperIndex+1)+". argument!", null);
					}
				
				currentMethodHelperIndex++;
			}
			else {
				report_error("Greska tip 2 na liniji "+singlePar.getLine(), null);
			}
			//currentMethodHelper.types.get(currentMethodHelperIndex);
		}
	}
	
	
	// act pars stuff end
	

}
