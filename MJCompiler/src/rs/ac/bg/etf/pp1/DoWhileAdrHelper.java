package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

import rs.etf.pp1.mj.runtime.Code;

public class DoWhileAdrHelper {

	public int beginningAdr;
	public int afterWhileAdr;
	public int conditionAdr;
	
	ArrayList<Integer> adressesToPatchBeginning = new ArrayList<>();
	ArrayList<Integer> adressesToPatchAfterWhile = new ArrayList<>();
	int patchJmpToCondition;
	
	public void patchAdresses() {
		for (int adr: adressesToPatchBeginning) {
			int temp = Code.pc;
			Code.pc = beginningAdr;
			Code.fixup(adr);
			Code.pc = temp;
		}
		
		for (int adr: adressesToPatchAfterWhile) {
			int temp = Code.pc;
			Code.pc = afterWhileAdr;
			Code.fixup(adr);
			Code.pc = temp;
		}
		
		int temp = Code.pc;
		Code.pc = conditionAdr;
		Code.fixup(patchJmpToCondition);
		Code.pc = temp;
		
	}
	
}
