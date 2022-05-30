// IMPORT SEKCIJA

// nasu implementaciju ubacujemo u sledeci paket
package rs.ac.bg.etf.pp1;
// importujemo ovu klasu zbog kasnijeg generisanja koda
import java_cup.runtime.Symbol;

%%
// SEKCIJA DIREKTIVA

%{ // kod sekcija - ovo ce ubaciti u kasnije generisanu klasu

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type){ // ova Symbol klasa se nalazi u Java cup paketu
		return new Symbol(type, yyline + 1, yycolumn);
	}
	
	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value){
		return new Symbol(type, yyline + 1, yycolumn, value);
	}

%}

%cup // jer leksicki analizator treba da bude cup-kompatibilan
%line // jer zelimo da moze da broji linije,
%column // a i kolone

// sluzi za dodavanje stanja za obradu komentara
%xstate COMMENT 

%eofval{ // sta lekser radi kada dodje do kraja fajla
	return new_symbol(sym.EOF);
%eofval}

%%
// REGULARNI IZRAZI

" "			{ }
"\b"		{ }
"\t"		{ }
"\r\n"		{ }
"\f"		{ }


"program"		{ return new_symbol(sym.PROG, yytext()); }
"break"			{ return new_symbol(sym.BREAK, yytext()); }
"class"			{ return new_symbol(sym.CLASS, yytext()); }
"enum"			{ return new_symbol(sym.ENUM, yytext()); }
"else"			{ return new_symbol(sym.ELSE, yytext()); }
"const"			{ return new_symbol(sym.CONST, yytext()); }
"if"			{ return new_symbol(sym.IF, yytext()); }
"do"			{ return new_symbol(sym.DO, yytext()); }
"while"			{ return new_symbol(sym.WHILE, yytext()); }
"new"			{ return new_symbol(sym.NEW, yytext()); }
"print"			{ return new_symbol(sym.PRINT, yytext()); }
"read"			{ return new_symbol(sym.READ, yytext()); }
"return"		{ return new_symbol(sym.RETURN, yytext()); }
"void"			{ return new_symbol(sym.VOID, yytext()); }
"extends"		{ return new_symbol(sym.EXTENDS, yytext()); }
"continue"		{ return new_symbol(sym.CONTINUE, yytext()); }
"this"			{ return new_symbol(sym.THIS, yytext()); }
"super"			{ return new_symbol(sym.SUPER, yytext()); }
"goto"			{ return new_symbol(sym.GOTO, yytext()); }
"record"		{ return new_symbol(sym.RECORD, yytext()); }

"..."			{ return new_symbol(sym.TRIPLEDOT, yytext()); }
"++"			{ return new_symbol(sym.INCREMENT, yytext()); }
"--"			{ return new_symbol(sym.DECREMENT, yytext()); }
"=="			{ return new_symbol(sym.DOUBLEEQUAL, yytext()); }
"!="			{ return new_symbol(sym.NOTEQUAL, yytext()); }
"<="			{ return new_symbol(sym.SMALLEREQUAL, yytext()); }
">="			{ return new_symbol(sym.BIGGEREQUAL, yytext()); }
"&&"			{ return new_symbol(sym.ANDAND, yytext()); }
"||"			{ return new_symbol(sym.OROR, yytext()); }
"-"				{ return new_symbol(sym.MINUS, yytext()); }
"*"				{ return new_symbol(sym.MUL, yytext()); }
"/"				{ return new_symbol(sym.DIV, yytext()); }
"%"				{ return new_symbol(sym.MOD, yytext()); }
">"				{ return new_symbol(sym.BIGGER, yytext()); }
"<"				{ return new_symbol(sym.SMALLER, yytext()); }
":"				{ return new_symbol(sym.COLON, yytext()); }
"."				{ return new_symbol(sym.DOT, yytext()); }
"+"				{ return new_symbol(sym.PLUS, yytext()); }
"="				{ return new_symbol(sym.EQUAL, yytext()); }
";"				{ return new_symbol(sym.SEMI, yytext()); }
","				{ return new_symbol(sym.COMMA, yytext()); }
"("				{ return new_symbol(sym.LPAREN, yytext()); }
")"				{ return new_symbol(sym.RPAREN, yytext()); }
"{"				{ return new_symbol(sym.LBRACE, yytext()); }
"}"				{ return new_symbol(sym.RBRACE, yytext()); }
"["				{ return new_symbol(sym.LBRACKET, yytext()); }
"]"				{ return new_symbol(sym.RBRACKET, yytext()); }

"//" 			{ yybegin(COMMENT); }
<COMMENT> . { yybegin(COMMENT); } // ako u stanju COMMENT procitas bilo koji znak, ostani u tom stanju
<COMMENT> "\r\n" { yybegin(YYINITIAL); } // ako u stanju COMMENT predjes u novi red, predji u stanje YYINITIAL

[0-9]+ { return new_symbol(sym.NUMBER, new Integer(yytext())); } // celobrojne konstante
(\')(([\x20-\x21]|[\x23-\x7F]){1, 1})(\')		{ return new_symbol(sym.CHAR, yytext()); }
("true"|"false")		{ return new_symbol(sym.BOOL, yytext()); }

([a-z]|[A-Z])[a-zA-Z0-9_]*  { return new_symbol(sym.IDENT, yytext()); } // identifikatori

.	{ System.err.println("Leksicka greska ("+yytext()+"), linija "+(yyline+1)+", kolona "+(yycolumn+1)); } // ukoliko je procitan nevalidni token

