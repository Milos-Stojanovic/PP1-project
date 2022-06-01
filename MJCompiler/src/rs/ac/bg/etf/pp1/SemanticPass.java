package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticPass extends VisitorAdaptor {
	
	Struct currentStruct = null;
	
	Logger log = Logger.getLogger(getClass());
	
	public void report_error(String message, SyntaxNode info) {
//		errorDetected = true;
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
		progName.obj = Tab.insert(Obj.Prog, progName.getName(), Tab.noType);
		Tab.openScope();
	}
	
	public void visit(Type type) { 
		currentStruct = Tab.find(type.getI1()).getType();
	}
	
	public void visit(VarElem varElem) {
		Obj obj = Tab.find(varElem.getI1());
		
		if(obj != Tab.noObj) {
			// error
			report_error("postoji promenljiva", varElem);
		}
		else {
			// da li je niz
			boolean isArray = varElem.getOptArray() instanceof Array;
			
			if (isArray) {
				Struct myStruct = new Struct(Struct.Array, currentStruct);
				Tab.insert(Obj.Var, varElem.getI1(), myStruct);
			} 
			else {
				Tab.insert(Obj.Var, varElem.getI1(), currentStruct);
			}
		
		}
		
	}
	
	public void visit(VarDecl varDecl) {
		currentStruct = null;
	}

}
