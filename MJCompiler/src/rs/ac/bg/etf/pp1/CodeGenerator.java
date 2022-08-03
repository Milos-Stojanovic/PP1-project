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
	
	private ArrayList<MyLabel> localLabelsGoto = null;
	private ArrayList<MyLabel> localLabelsDest = null;
	
	private ArrayList<ElseThenAdrHelper> ifElseAdrHelper = new ArrayList<>();
	private int ifElseIndex = -1;
	
	private ArrayList<DoWhileAdrHelper> doWhileAdrHelper = new ArrayList<>();
	private int doWhileIndex = -1;
	
	private ArrayList<String> IfElse_or_DoWhile = new ArrayList<>();
	private int condIndex = -1;
	
	public int getMainPc() {
		return mainPc;
	}
	
	public void visit(ProgName ProgramName) {
        super.visit(ProgramName);
        
      //len -> duzinu
      		Obj obj = Tab.find("len");
      		obj.setAdr(Code.pc);
      		Code.put(Code.enter);
      		Code.put(obj.getLevel());
      		Code.put(obj.getLocalSymbols().size());
      		Code.put(Code.load_n);
      		Code.put(Code.arraylength);
      		Code.put(Code.exit);
      		Code.put(Code.return_);
      		
      		//chr 
      		obj =Tab.find("chr");
      		obj.setAdr(Code.pc);
      		Code.put(Code.enter);
      		Code.put(obj.getLevel());
      		Code.put(obj.getLocalSymbols().size());
      		Code.put(Code.load_n);
      		Code.put(Code.exit);
      		Code.put(Code.return_);
      		
      		// ord -> vraca ascii
      		obj = Tab.find("ord");
      		obj.setAdr(Code.pc);
      		Code.put(Code.enter);
      		Code.put(obj.getLevel());
      		Code.put(obj.getLocalSymbols().size());
      		Code.put(Code.load_n);
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
		
		// initializing an array of labels in this method
		localLabelsGoto = new ArrayList<>();
		localLabelsDest = new ArrayList<>();
		
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
		
		//System.out.println(localLabelsGoto);
		//System.out.println(localLabelsDest);
		
		// patching goto & destination label adresses
		for(MyLabel gotoLabel: localLabelsGoto) {
			for(MyLabel destLabel: localLabelsDest) {
				if (gotoLabel.getName().equals(destLabel.getName())) { // patch address for goto command
					int oldPc = Code.pc;

					Code.pc = destLabel.getAdr();
					Code.fixup(gotoLabel.getAdr());
					
					Code.pc = oldPc;
				}
			}
		}
		// patching goto & destination label adresses
		
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
	
//	public void visit(Designator designator) {
//		
//	}
	
	public void visit(DesignatorStatement dsgStmt) {
		
		if(dsgStmt.getDsgArray() instanceof DsgArrayAE) {
			if(((DsgArrayAE)dsgStmt.getDsgArray()).getExpr().getTermArray() instanceof SingleTerm
					&& ((SingleTerm)((DsgArrayAE)dsgStmt.getDsgArray()).getExpr().getTermArray()).getTerm() instanceof SingleFactor
					&& ((FactorNoMinus)((SingleFactor)((SingleTerm)((DsgArrayAE)dsgStmt.getDsgArray()).getExpr().getTermArray()).getTerm()).getFactorUn()).getFactor() instanceof FactorNew) {
				
				Obj dsg = dsgStmt.getDesignator().obj;
				Code.put(Code.newarray);
				
				if(dsg.getType().getElemType() == Tab.charType) {
					Code.put(0);
				}
				else {
					Code.put(1);
				}
				Code.store(dsg);
				
			}
			else {
				Obj dsg = dsgStmt.getDesignator().obj;
				// provera da li je levo od = element niza ili nije
				System.out.println("ASDASDASDA  "+dsg.getType().getKind());
				if(dsg.getType().getElemType() == null || !(dsgStmt.getDesignator().getDsgOpt() instanceof DesignatorOpt)) {
					Code.store(dsg);
				}
				else {
					Code.load(dsg);
					Code.put(Code.dup_x2);
					Code.put(Code.pop);
					if(dsg.getType().getElemType() == Tab.charType) { // ako je u pitanju niz char-ova
						Code.put(Code.bastore);
					}
					else { // ako je u pitanju niz int-ova
						Code.put(Code.astore);
					}
				}
			}
		}
		if(dsgStmt.getDsgArray() instanceof DsgArrayActPars) { // DONE
			// za slucaj pozivanja metode
			/*if(dsgStmt.getDesignator().obj.getName().equals("len")) {
				Code.loadConst(dsgStmt.getDesignator().obj.getAdr());
				Code.put(Code.arraylength);
				return;
			}*/
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
			if(dsg.getType().getKind() != 3) {
				Code.load(dsg);
				Code.loadConst(1);
				Code.put(Code.add);
				Code.store(dsg);
			}
			else {
				Code.load(dsg);
				Code.put(Code.dup_x1);
				Code.put(Code.pop);
				Code.put(Code.dup2);
				Code.put(Code.aload);
				Code.put(Code.const_1);
				Code.put(Code.add);
				Code.put(Code.astore);
			}
		}
		if(dsgStmt.getDsgArray() instanceof DsgArrayDec) { // DONE
			Obj dsg = dsgStmt.getDesignator().obj;
			if(dsg.getType().getKind() != 3) {
				Code.load(dsg);
				Code.loadConst(1);
				Code.put(Code.sub);
				Code.store(dsg);
			}
			else {
				Code.load(dsg);
				Code.put(Code.dup_x1);
				Code.put(Code.pop);
				Code.put(Code.dup2);
				Code.put(Code.aload);
				Code.put(Code.const_1);
				Code.put(Code.sub);
				Code.put(Code.astore);
			}
		}
		
	}
	
	public void visit(FactorDesg dsg) {
		if(dsg.getFactorOptActPars() instanceof NoFactorOptActPars) {
			if(dsg.getDesignator().getDsgOpt() instanceof DesignatorOpt) { // u pitanju je element niza
				Code.load(dsg.getDesignator().obj);
				Code.put(Code.dup_x1);
				Code.put(Code.pop);
				if(dsg.getDesignator().obj.getType().getElemType() == Tab.charType) { // ako je u pitanju niz char-ova
					Code.put(Code.baload);
				}
				else { // ako je u pitanju niz int-ova
					Code.put(Code.aload);
				}
			}
			else
				Code.load(dsg.getDesignator().obj);
		}
		else {
			// za slucaj pozivanja metode
			/*if(dsg.getDesignator().obj.getName().equals("len")) {
				Code.loadConst(dsg.getDesignator().obj.getAdr());
				Code.put(Code.arraylength);
				return;
			}*/
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
		if (StatementRead.getDesignator().obj.getType() == Tab.charType) {
	        Code.put(Code.bread);
	        Code.store(StatementRead.getDesignator().obj);
		}
		else {
			if(StatementRead.getDesignator().obj.getType().getKind() != 3) {
				Code.put(Code.read);
				Code.store(StatementRead.getDesignator().obj);
			}
			else {
				if(StatementRead.getDesignator().obj.getType().getElemType() == Tab.charType) {
					Code.load(StatementRead.getDesignator().obj);
					Code.put(Code.dup_x1);
					Code.put(Code.pop);
					Code.put(Code.bread);
					Code.put(Code.bastore);
				}
				else {
					Code.load(StatementRead.getDesignator().obj);
					Code.put(Code.dup_x1);
					Code.put(Code.pop);
					Code.put(Code.read);
					Code.put(Code.astore);
				}
			}
        }
    }
	
	
	// label handling
	
	public void visit(SingleStmtElemGoto gotoStmt) {
		localLabelsGoto.add(new MyLabel(gotoStmt.getLabel().getI1(), Code.pc+1));
		Code.putJump(0);
	}
	
	public void visit(LabelStmt labelStmt) {
		localLabelsDest.add(new MyLabel(labelStmt.getLabel().getI1(), Code.pc));
	}
	
	// label handling
	
	
	// if-else handling
		
	public int getMyOp(Relop relOp) {
		if (relOp instanceof RelopBigger) return Code.gt;
		if (relOp instanceof RelopBiggerEqual) return Code.ge;
		if (relOp instanceof RelopSmaller) return Code.lt;
		if (relOp instanceof RelopSmallerEqual) return Code.le;
		if (relOp instanceof ArrayRelop1) {
			if (((ArrayRelop1)relOp).getArrayRelop() instanceof RelopDoubleEqual) return Code.eq;
			else return Code.ne;
		}
		return 0;
	}
	
	public int getMyOp(ArrayRelop relOp) {
		if (relOp instanceof RelopDoubleEqual) return Code.eq;
		if (relOp instanceof RelopNotEqual) return Code.ne;
		return 0;
	}
	
	public void visit(IfLparen lp) {
		ifElseAdrHelper.add(new ElseThenAdrHelper());
		ifElseIndex++;
		IfElse_or_DoWhile.add("IfElse");
		condIndex++;
	}
	
	public void visit(Helper help) { // help for patching addresses once out of if-else block
		ifElseAdrHelper.get(ifElseIndex).afterElseAdr = Code.pc;
		if(ifElseAdrHelper.get(ifElseIndex).elseAdr == -1) ifElseAdrHelper.get(ifElseIndex).elseAdr = ifElseAdrHelper.get(ifElseIndex).afterElseAdr; // if there was no else block
		ifElseAdrHelper.get(ifElseIndex).patchAdresses();
		
		if(ifElseIndex >= 0) {
			ifElseAdrHelper.remove(ifElseIndex);
			ifElseIndex--;
		}
	}
	
	public void visit(IfRparen rp) {
		
		ifElseAdrHelper.get(ifElseIndex).thenAdr = Code.pc;
		if (patchToNextOR != null && patchToNextOR.size() > 0) {
			for (int i = 0; i < patchToNextOR.size(); i++) {
				ifElseAdrHelper.get(ifElseIndex).adressesToPatchElse.add(patchToNextOR.get(i));
			}
		}
		
		//ifElseAdrHelper.get(ifElseIndex).adressesToPatchElse.add(Code.pc-2);
		
		
		patchToNextOR = new ArrayList<>();
		if(condIndex >= 0) {
			IfElse_or_DoWhile.remove(condIndex);
			condIndex--;
		}
	}
	
	public int getInverse(int num) {
		switch (num) {
		 case 43:
			 return 44;
		 case 44:
			 return 43;
		 case 45:
			 return 48;
		 case 46:
			 return 47;
		 case 47:
			 return 46;
		 case 48:
			 return 45;
		}
		return 0;
	}
	
	public void visit(ElseHelp eh) {
		Code.putJump(0);
		ifElseAdrHelper.get(ifElseIndex).adressesToPatchAfterElse.add(Code.pc-2);
		ifElseAdrHelper.get(ifElseIndex).elseAdr = Code.pc;
	}
	
	ArrayList<Integer> patchToNextOR = new ArrayList<>();
	int nextOrAdr = -1;
	public void patchAdressesNextOR() {
		for (int adr: patchToNextOR) {
			int temp = Code.pc;
			Code.pc = nextOrAdr;
			Code.fixup(adr);
			Code.pc = temp;
		}
	}
	
	private int cnt = 0;
	public void visit(OrHelp help) {
		
		if (patchToNextOR != null && patchToNextOR.size()>0) {
			nextOrAdr = Code.pc;
			patchAdressesNextOR(); 
		} 

		
		int negOpCode = Code.get(Code.pc-3);
		Code.buf[Code.pc-3] = (byte)getInverse(negOpCode);
		
		if (IfElse_or_DoWhile.get(condIndex).equals("IfElse"))
			ifElseAdrHelper.get(ifElseIndex).adressesToPatchThen.add(Code.pc-2);
		if (IfElse_or_DoWhile.get(condIndex).equals("DoWhile"))
			doWhileAdrHelper.get(doWhileIndex).adressesToPatchBeginning.add(Code.pc-2);
		
		patchToNextOR = new ArrayList<>();
	}
	
	public void visit(SingleCondFact condFact) {
		Code.loadConst(1);
		Code.putFalseJump(Code.eq, 0);
		patchToNextOR.add(Code.pc-2);
	}
	
	public void visit(DoubleCondFact condFact) {
		Code.putFalseJump(this.getMyOp(condFact.getRelop()), 0);
		
		patchToNextOR.add(Code.pc-2);
	}
	
	// if-else handling
	
	
	// do-while handling
	
	public void visit(DoWhileBegin dwb) {
		Code.put(Code.jmp);
		Code.put2(0);
		doWhileAdrHelper.add(new DoWhileAdrHelper());
		doWhileIndex++;
		doWhileAdrHelper.get(doWhileIndex).beginningAdr = Code.pc;
		doWhileAdrHelper.get(doWhileIndex).patchJmpToCondition = Code.pc-2;
		IfElse_or_DoWhile.add("DoWhile");
		condIndex++;
	}
	
	public void visit(DoWhileLparen dwl) {
		doWhileAdrHelper.get(doWhileIndex).conditionAdr = Code.pc;
	}
	
	public void visit(DoWhileRparen dwr) {
		
		if (patchToNextOR != null && patchToNextOR.size()-1 > 0) {
			for (int i = 0; i < patchToNextOR.size()-1; i++) {
				doWhileAdrHelper.get(doWhileIndex).adressesToPatchAfterWhile.add(patchToNextOR.get(i));
			}
		}
		Code.buf[Code.pc-3] = (byte)getInverse(Code.buf[Code.pc-3]);
		doWhileAdrHelper.get(doWhileIndex).adressesToPatchBeginning.add(Code.pc-2);
		
		
		patchToNextOR = new ArrayList<>();
		doWhileAdrHelper.get(doWhileIndex).afterWhileAdr = Code.pc;
		doWhileAdrHelper.get(doWhileIndex).patchAdresses();
		
		
		if(doWhileIndex >= 0) {
			doWhileAdrHelper.remove(doWhileIndex);
			doWhileIndex--;
		}
		if(condIndex >= 0) {
			IfElse_or_DoWhile.remove(condIndex);
			condIndex--;
		}
		
	}
	
	public void visit(SingleStmtElemBreak breakStmt) {
		Code.put(Code.jmp);
		Code.put2(0);
		doWhileAdrHelper.get(doWhileIndex).adressesToPatchAfterWhile.add(Code.pc-2);
	}
	
	public void visit(SingleStmtElemContinue continueStmt) {
		Code.put(Code.jmp);
		Code.put2(0);
		doWhileAdrHelper.get(doWhileIndex).adressesToPatchBeginning.add(Code.pc-2);
	}
	
	// do-while handling
	
	public void visit(FactorMinus fm) {
		Code.loadConst(-1);
		Code.put(Code.mul);
	}
	
	
}
