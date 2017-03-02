module FISCHER-AGENT-SYNTAX 
  syntax Allcode ::= Agent Allwrite Allread Local Botcode Simsettings 

  syntax Simsettings ::= "NAgents" Int  ";"

  syntax Br ::= ":"
  syntax Agent ::= "agent" "::" "Fischer"


  syntax Vardecl  ::= Type Id ";"
                    | Type Id "=" Exp ";"       [strict(3)]
  
  syntax Vardecls ::= List{Vardecl, ""}         [klabel('VarDecls)]



  syntax Allwrite ::= "allwrite" Br Vardecls


  syntax Allread ::= "allread" Br Vardecls

  
  syntax Botcode ::= Init EventBlock

  syntax Local ::= "local" Br Vardecls 


  
  syntax Init ::= "init" Br


  syntax EventBlock ::= "events" Br Events 


  syntax Events ::= List{Event, ""} 	[klabel('Vardecls')]

  syntax Id ::= 'agentId' | 'numBots' 

  syntax Type    ::= "int" | "float" | "bool"

  syntax Event   ::= Id "()" Br "pre" "(" Exp ")" ";" "eff" Br Stmts    [strict(2)]
 
  syntax AgentId ::= "agentid"

  syntax Exp     ::= Bool | Int | Float | Id | AgentId
                   | "(" Exp ")"   [bracket]
		   | "rand" "(" Int ")" 
                   | Exp "*" Exp   [strict, left]
                   > Exp "+" Exp   [strict, left]
                   | Exp "-" Exp   [strict, left]
                   > Exp "<"  Exp  [seqstrict, non-assoc]
                   | Exp "<=" Exp  [seqstrict, non-assoc]
                   | Exp ">"  Exp  [seqstrict, non-assoc]
                   | Exp ">=" Exp  [seqstrict, non-assoc]
                   | Exp "==" Exp  [seqstrict, non-assoc]
                   | Exp "!=" Exp  [seqstrict, non-assoc]
                   > "not" Exp     [strict]
                   > Exp "and" Exp [strict(1), left]
                   > Exp "=" Exp   [strict(2), right]


  syntax Stmt  ::=
                  Exp ";"                             [strict] 
		  | "if" "(" Exp ")" Br Stmts "else" Br Stmts [strict(1)]
  syntax Stmts ::= List{Stmt, ""}                [klabel('Stmts)]
endmodule


