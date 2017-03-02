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

module ROBOT-DSM
  imports ROBOT-SYNTAX
  
  syntax Stage ::= "Preprocessing"
                 | "Simulation"

  syntax State ::= "Discrete"
                 | "Progress"

 
  configuration
    <T>
      <robots>
        <robot multiplicity = "*">
          <k> $PGM:K  </k>
          <env> .Map </env>
          <id> 0 </id>
          <period> 0 </period>
          <localtime> 0 </localtime>
          <request> 0 </request>
        </robot>
      </robots>
      
      // memory related cells
      <mwEnv> .Map </mwEnv>
      <store> .Map </store>
      <nextLoc> 0 </nextLoc>
      // random delay
      <delay> 0 </delay>
      // calendar automata
      <timeout> .List </timeout>
      <time> 0 </time>
      <maxTime> 0 </maxTime>
      <state> Progress </state>
      // global lock req queue
      <busy> .K </busy>
      <lockReq> .List </lockReq>
      // robot
      <nbots> 1 </nbots>
      <livebots> 0 </livebots>
      <initialized> 0 </initialized>
      // code shared among robots
      <varCode> .K </varCode>
      <stepCode> .K </stepCode>
      // execution stage
      <stage> Preprocessing </stage>
    </T>


  syntax Val  ::= Int | Bool | String
  syntax Vals ::= List{Val, ","}
  syntax Exp  ::= Val
  syntax KResult ::= Val
  syntax EventType ::= "NodeAct" | "LockAct"
  //Event - EventType, EventId, Timeout
  syntax Event ::= "event" "(" EventType "," Int "," Int ")"

  // Sequential composition
  rule .Stmts => .                                     [structural]
  rule S:Stmt Ss:Stmts => S ~> Ss                      [structural]
  rule .VarDecls => .                                  [structural]
  rule V:VarDecl Vs:VarDecls => V ~> Vs                [structural]

  // Variable Declaration
  syntax KItem ::= undefined(Type)
  rule <robot>...
         <k> T:Type X:Id; => . ...</k>
         <env> Env => Env[X <- L] </env>
       ...</robot>
       <store> ... .Map => L |-> undefined(T) ...</store>
       <nextLoc> L:Int => L +Int 1 </nextLoc>

  rule <robot>...
         <k> T:Type X:Id = V:Val; => . ...</k>
         <env> Env => Env[X <- L]</env>
       ...</robot>
       <store> ... .Map => L |-> V ...</store>
       <nextLoc> L:Int => L +Int 1 </nextLoc>

  // Preprocessing stage
  rule <k> SDs:SharedDecls VDs:VarDecls SF:StepFunc N:Nbots
           => SDs ~> N ...</k>
       <varCode> .K => VDs </varCode>
       <stepCode> .K => SF </stepCode>
       <stage> Preprocessing </stage>

  syntax KItem ::= "endMW"
  rule <robot>...
         <k> MW {VDs:VarDecls} => VDs ~> endMW...</k>
       ...</robot>
       <stage> Preprocessing </stage>

  rule <robot>...
         <k> endMW =>. ...</k>
         <env> ENV </env>
       ...</robot>
       <mwEnv> .Map => ENV </mwEnv>
       <stage> Preprocessing </stage>

  rule <robot>...
         <k> Sim N {P:Int, Ps:Periods} {D:Int, Ds:Delays} MT:Int; => Sim N -Int 1 {Ps} {Ds} MT; ...</k>
         <env> E </env>
       ...</robot>
       (.Bag => <robot> ...
                  <k> VC ~> initialized </k>
                  <period> P </period>
                  <env> E </env>
                  <id> ID </id>
                ...</robot>)
       <timeout> ... .List => ListItem(event(NodeAct, ID, D)) </timeout>
       <nbots> ID:Int => ID +Int 1 </nbots>
       <varCode> VC </varCode>
       <stage> Preprocessing </stage>                 requires N =/=K 1

  syntax KItem ::= "initialized"
  rule <robot>...
        <k> Sim 1 {P:Int} {D:Int} MT; => VC ~> initialized...</k>
        <period> _ => P </period>
        <id> ID </id>
       ...</robot>
       <varCode> VC </varCode>
       <timeout> ... .List => ListItem(event(NodeAct, ID, D)) </timeout>
       <maxTime> 0 => MT </maxTime>
       <stage> Preprocessing </stage>

  rule <k> initialized => . ...</k>
       <initialized> N => N +Int 1 </initialized> 

  rule <nbots> N </nbots>
       <initialized> N </initialized>
       <delay> 0 => randomRandom(10) +Int 1 </delay>
       <stage> Preprocessing => Simulation </stage>

  // Simulation stage
  syntax Int ::= getTimeout(Event)                                         [function]
  syntax Int ::= getEventID(Event)                                         [function]
  syntax EventType ::= getEventType(Event)                                 [function]
  rule getTimeout(event(ET, EID, T)) => T
  rule getEventID(event(ET, EID, T)) => EID
  rule getEventType(event(ET, EID, T)) => ET 

  syntax Int ::= findMin(List)                                             [function]
               | "Nat"
  rule findMin(.List) => Nat
  rule findMin(ListItem(E)) => getTimeout(E)
  rule findMin(ListItem(E1) ListItem(E2) Ls:List) => findMin(ListItem(E1) Ls)      requires getTimeout(E1) <=Int getTimeout(E2)
  rule findMin(ListItem(E1) ListItem(E2) Ls:List) => findMin(ListItem(E2) Ls)      requires getTimeout(E1) >Int getTimeout(E2)

  // Time progress transition  
  rule <timeout> TO:List </timeout>
       <state> Progress => Discrete </state>
       <time> T => findMin(TO) </time>
       <maxTime> MT </maxTime>                           
       <stage> Simulation </stage>                                               requires T <Int findMin(TO) andBool T <=Int MT

  // Discrete time transition
  syntax Int ::= findTimeout(List, Int, EventType)                               [function]
  rule findTimeout(.List, EID, ET) => Nat 
  rule findTimeout(ListItem(E) Ls:List, EID, ET) => getTimeout(E)                          requires EID ==Int getEventID(E) andBool getEventType(E) ==K ET
  rule findTimeout(ListItem(E) Ls:List, EID, ET) => findTimeout(Ls, EID, ET)               requires (notBool(EID ==Int getEventID(E) andBool getEventType(E) ==K ET))

  syntax List ::= removeItem(List, Int, EventType)                               [function]
  rule removeItem(ListItem(E) Ls:List, EID, ET) => Ls                                      requires getEventID(E) ==Int EID andBool getEventType(E) ==K ET
  rule removeItem(ListItem(E) Ls:List, EID, ET) => ListItem(E) removeItem(Ls, EID, ET)     requires notBool(getEventID(E) ==Int EID andBool getEventType(E) ==K ET)

  rule <robot>...
         <k> .K => Code </k>
         <localtime> LocalTime => GlobalTime </localtime>
         <id> ID </id>
       ...</robot>
       <timeout> TO => removeItem(TO, ID, NodeAct) </timeout>
       <stepCode> Code </stepCode>
       <time> GlobalTime </time>
       <livebots> LBots:Int => LBots +Int 1 </livebots>
       <state> Discrete </state>
       <stage> Simulation </stage>                                               requires LocalTime <Int GlobalTime andBool findTimeout(TO, ID, NodeAct) ==Int GlobalTime

  // Variable lookup
  rule <robot>...
         <k> X:Id => V ...</k>
         <env>... X |-> L ...</env>
       ...</robot>
       <store>... L |-> V:Val ...</store>

  // Assignment
  syntax Exp ::= lvalue(K)
  syntax Val ::= loc(Int)
  context (HOLE => lvalue(HOLE)) = _
 
  rule <k> lvalue(X:Id => loc(L)) ...</k>
       <env>... X |-> L:Int ...</env>

  rule <k> loc(L) = V:Val => V ...</k>
       <store> ... L |-> (_ => V) ...</store>

  // Expression Statements
  rule _:Val ; => .
  
  // Arithmetic Operator
  rule I1 + I2 => I1 +Int I2
  rule Str1 + Str2 => Str1 +String Str2
  rule I1 - I2 => I1 -Int I2
  rule I1 * I2 => I1 *Int I2
  rule I1 < I2 => I1 <Int I2
  rule I1 <= I2 => I1 <=Int I2
  rule I1 > I2 => I1 >Int I2
  rule I1 >= I2 => I1 >=Int I2

  // Equality
  rule V1:Val == V2:Val => V1 ==K V2

  // Logical Operator
  rule not T => notBool(T)
  rule true and E => E
  rule false and _ => false

  // Evaluating Events one by one
  syntax KItem ::= "sleep"
                 | "releaseLock"
  rule Step(){Evs:Events} => Evs ~> releaseLock ~> sleep
  
  rule .Events => .

  syntax KItem ::= closure(Exp, Stmts, Events) 
  rule X:Id() {pre (E:Exp); eff {Ss:Stmts}} Evs:Events => closure(E, Ss, Evs)

  context closure(HOLE:Exp, Ss, Evs)

  rule closure(true, Ss, Evs) => Ss
  rule closure(false, Ss, Evs) => Evs

  rule <robot>...
         <k> releaseLock =>. ...</k>
         <id> ID </id>
       ...</robot>
       <busy> ID => .K </busy>

  rule <robot>...
         <k> releaseLock =>. ...</k>
         <id> ID </id>
       ...</robot>
       <busy> B </busy>                                         requires ID =/=K B

  /*rule <robot>...
         <k> sleep =>. ...</k>
         <period> P </period>
         <id> ID </id>
       ...</robot>
       <livebots> N => N -Int 1 </livebots>
       <timeout>... ID |-> (T => T +Int P) ...</timeout>*/

  // After robots finish, set Discrete to Progress
  rule <livebots> 0 </livebots>
       <timeout> TO </timeout>
       <time> GT </time>
       <state> Discrete => Progress </state>                    requires  GT <Int findMin(TO)

  // Lock semantics
  rule <robot>...
         <k> atomic{Ss:Stmts} => . ...</k>
         <id> ID </id>
       ...</robot>
       <lockReq> QID (.List => ListItem(ID)) </lockReq>
       <delay> D => randomRandom(10) + 1 </delay>
       <busy> B </busy>                                         requires (notBool (ID in QID)) andBool ID =/=K B

  /*rule <robot>...
         <k> atomic{Ss:Stmts} => . ...</k>
         <id> ID </id>
       ...</robot>
       <queue> QID </queue>                                     
       <busy> B </busy>                                         requires (ID in QID) andBool ID =/=K B

  rule <robot>...
         <k> atomic{Ss:Stmts} => Ss ...</k>
         <id> ID </id>
       ...</robot>
       <busy> ID </busy>*/

endmodule
