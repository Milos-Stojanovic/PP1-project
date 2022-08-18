package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

import rs.etf.pp1.mj.runtime.Code;

public class DoWhileAdrHelper {

	public int beginningAdr;
	public int afterWhileAdr;
	
	ArrayList<Integer> adressesToPatchBeginning = new ArrayList<>();
	ArrayList<Integer> adressesToPatchAfterWhile = new ArrayList<>();
	
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
		
	}
	
}
