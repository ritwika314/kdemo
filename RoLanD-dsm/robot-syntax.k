module ROBOT-SYNTAX

  syntax Sim     ::= SharedDecls BotCode Nbots

  //For initializing the number of robots and corresponding periods
  syntax Nbots   ::= "Sim" Int "{" Periods "}" "{" Delays "}" Int ";"
  //The unit for period/delay is ms
  syntax Periods ::= List{Int,","}              [klabel('Periods)]
  syntax Delays  ::= List{Int,","}              [klabel('Delays)]

  syntax SharedDecls ::= MwDecl
 
  syntax BotCode  ::= VarDecls StepFunc

  syntax StepFunc ::= "Step()" "{" Events "}"

  syntax MwDecl   ::= "MW" "{" VarDecls "}"

  syntax VarDecl  ::= Type Id ";"
                    | Type Id "=" Exp ";"       [strict(3)]
  
  syntax VarDecls ::= List{VarDecl, ""}         [klabel('VarDecls)]

  syntax Type    ::= "int" | "float" | "boolean"

  syntax Event   ::= Id "()" "{" "pre" "(" Exp ")" ";" "eff" Block "}"   [strict(2)]
  syntax Events  ::= List{Event,""}             [klabel('Events)]

  syntax Id      ::= "robotIndex" | "NumBots"

  syntax Exp     ::= Bool | Int | Float | Id
                   | "(" Exp ")"   [bracket]
                   | Exp "*" Exp   [strict, left]
                   > Exp "+" Exp   [strict, left]
                   | Exp "-" Exp   [strict, left]
                   > Exp "<"  Exp  [seqstrict, non-assoc]
                   | Exp "<=" Exp  [seqstrict, non-assoc]
                   | Exp ">"  Exp  [seqstrict, non-assoc]
                   | Exp ">=" Exp  [seqstrict, non-assoc]
                   | Exp "==" Exp  [seqstrict, non-assoc]
                   > "not" Exp     [strict]
                   > Exp "and" Exp [strict(1), left]
                   > Exp "=" Exp   [strict(2), right]

  syntax Block ::= "{" Stmts "}"

  syntax Stmt  ::= Block
                 | Exp ";"                             [strict] 
                 | "atomic" Block // shared memory communication
                 | "if" "(" Exp ")" Block "else" Block [strict(1)]

  syntax Stmts ::= List{Stmt, ""}                [klabel('Stmts)]

endmodule
