module ROBOT-SYNTAX

	syntax Val 
		
	syntax Sim ::= Nbots BotCode
	syntax BotCode ::= MwDecls SwDecls Decls "Init()" "{" Events "}"
	syntax Nbots ::= "Sim" Int ";" //for initializing the number of robots -_- . 
	syntax MwDecls ::= "MW" "{" Decls "}" 
	syntax SwDecls ::= "SW" "{" Decls "}"
	
	syntax Decl ::= Type Id ";" 
		       | Type Id "=" Exp  ";" [strict(3)]

	syntax Type ::= "int" | "float" | "bool"
	syntax Decls ::= List{Decl,""}
	syntax Event ::= Id"()" "{" "pre" "(" Exp ")" ";" "eff" Block "}"
	syntax Events ::= List{Event,""}
	syntax Exp ::= Val 
		    | Bool 
	            | Int  
                    | Id
		    | "(" Exp ")" [bracket]
		    | Exp "*" Exp [mul, left, strict]
		    > left : 
		      Exp "+" Exp [plus, left,strict]
		    > non-assoc: 
		      Exp "-" Exp [plus, left,strict]
		    > Exp "<" Exp [lt , seqstrict]
		    | Exp "<=" Exp [leq , seqstrict]
		    | Exp ">" Exp [gt , seqstrict]
		    | Exp ">=" Exp [geq , seqstrict]
		    | Exp "==" Exp [eq , seqstrict]
		    > "not" Exp [not, strict]	
		    > left :
		      Exp "and" Exp [and , strict(1)]
	

	syntax Stmt ::= Block   
		    | Id "=" Exp ";"
		    | "Atomic" Block // shared memory communication
		    | "if" "(" Exp ")" Block "else" Block [if, strict(1)]
	syntax Block ::= "{" Stmts "}"
	syntax KResult ::= Val 
	syntax Stmts ::= List{Stmt, ""}

	
endmodule 


module ROBOT
	imports ROBOT-SYNTAX
	
	configuration
		<T>
		<robot multiplicity = "*">
		<k> $PGM:Sim </k>
		<id>0 </id>
		<env> .Map </env>
		</robot>
		<time>
		0
		</time>
		</T>

	rule S1:Stmt S2:Stmt => S1 ~> S2 [structural]
	rule <k> Sim N:Int ; B:BotCode => Sim N -Int 1 ; B </k><env> E</env> (. => <robot><k>B</k><id> T:Int </id><env> E </env></robot>)  when N =/=K 1 andBool fresh(T)
	rule <k> Sim N:Int ; B:BotCode => B </k>  when N ==K 1 


	rule <k> ... X:Id => I ... </k> <env> ... X|-> I </env>
	rule <k> . T:Type X:Id = V:Val ; => . ... </k> <env> (. => X |-> V) ... </env> 
//rule <k> T:Type X:Id ; => .  ...</k> <env> . => X|->undefined ... </env>
	syntax Val ::= Int | Bool | Float

	//syntax KResult ::= Val 
endmodule 	
