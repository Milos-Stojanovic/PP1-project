package rs.ac.bg.etf.pp1;

import java.util.ArrayList;

import rs.etf.pp1.mj.runtime.Code;

public class ElseThenAdrHelper {

	public int elseAdr = -1;
	public int thenAdr;
	public int afterElseAdr;
	
	ArrayList<Integer> adressesToPatchElse = new ArrayList<>();
	ArrayList<Integer> adressesToPatchThen = new ArrayList<>();
	ArrayList<Integer> adressesToPatchAfterElse = new ArrayList<>();
	
	public void patchAdresses() {
		for (int adr: adressesToPatchElse) {
			int temp = Code.pc;
			Code.pc = elseAdr;
			Code.fixup(adr);
			Code.pc = temp;
		}
		
		for (int adr: adressesToPatchThen) {
			int temp = Code.pc;
			Code.pc = thenAdr;
			Code.fixup(adr);
			Code.pc = temp;
		}
		
		for (int adr: adressesToPatchAfterElse) {
			int temp = Code.pc;
			Code.pc = afterElseAdr;
			Code.fixup(adr);
			Code.pc = temp;
		}
	}
	
}
