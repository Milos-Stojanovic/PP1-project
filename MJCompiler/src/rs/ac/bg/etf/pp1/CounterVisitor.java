package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;

public class CounterVisitor extends VisitorAdaptor {

	protected int count;
	
	public int getCount() {
		return count;
	}
	
	public static class FormParamCounter extends CounterVisitor{
		
		public void visit(FormParsElem elem) {
			count++;
		}
		
		public void visit(OptArgsElem elem) {
			count++;
		}
	}
	
	
	public static class VarCounter extends CounterVisitor{
		
		public void visit(VarElem elem) {
			count++;
		}
	}
	
	public static class ActParsCounter extends CounterVisitor{
		
		public void visit(SingleActPars elem) {
			count++;
		}
		
		public void visit(MoreActPars elem) {
			count++;
		}
	}
}
