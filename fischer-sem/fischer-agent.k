requires "fischer-agent-syntax.k"


module FISCHER-AGENT
  imports FISCHER-AGENT-SYNTAX	
  
  syntax Stage ::= "Preprocessing" | "Simulation"

  syntax State ::= "Program" | "Environment"
 
  configuration
    <system>
      <agents>
	<agent multiplicity = "*">
	<k> $PGM:K </k>
        <id> 0 </id>
        <nrounds> 0 </nrounds>
	<maxrounds> 1 </maxrounds>
        <env> .Map </env>
        </agent>	
      </agents>
      <awEnv> .Map </awEnv>
      <store> .Map </store>
      <nextloc> 0 </nextloc>
      <time> 0 </time> 
     <state> Environment </state>
     <nbots> 1 </nbots>
     <livebots> 0 </livebots>
     <initialized> 0 </initialized>
     <varcode> .K </varcode>
     <botcode> .K </botcode>
     <stage> Preprocessing </stage>
     <maxtime> 100 </maxtime> 
    </system>
 

   syntax Val  ::= Int | Bool | String
   syntax Vals ::= List{Val, ","}
   syntax Exp  ::= Val
   syntax KResult ::= Val
 

   rule .Stmts => .                                     [structural]
   rule S:Stmt Ss:Stmts => S ~> Ss                      [structural]
   rule .Vardecls => .                                  [structural]
   rule V:Vardecl Vs:Vardecls => V ~> Vs                [structural]

   //testing

  syntax KItem ::= undefined(Type)
  rule <agent>...
         <k> T:Type X:Id; => . ...</k>
         <env> Env => Env[X <- L] </env>
       ...</agent>
       <store> ... .Map => L |-> undefined(T) ...</store>
       <nextloc> L:Int => L +Int 1 </nextloc>

   rule <agent>...
         <k> T:Type X:Id = V:Val; => . ...</k>
         <env> Env => Env[X <- L]</env>
       ...</agent>
       <store> ... .Map => L |-> V ...</store>
       <nextloc> L:Int => L +Int 1 </nextloc>

   rule <k> A:Agent Aw:Allwrite Ar:Allread local Br L:Vardecls I:Init E:EventBlock S:Simsettings => Aw ~> S ... </k>
       <varcode> .K => L </varcode>
       <botcode> .K => E </botcode>
       <stage> Preprocessing </stage>

   rule rand(I:Int) => I 
  syntax KItem ::= "endAW"
 
  rule <agent> ... 
         <k> allwrite : V:Vardecls=> V ~> endAW ... </k>
         ... </agent>
         <stage> Preprocessing </stage>


  rule <agent>...
         <k> endAW =>. ...</k>
         <env> ENV </env>
       ...</agent>
       <awEnv> .Map => ENV </awEnv>
       <stage> Preprocessing </stage>

  rule <agent>...
         <k> NAgents N:Int; => NAgents N -Int 1; ...</k>
         <env> E </env>
       ...</agent>
       (.Bag => <agent> ...
                  <k> VC ~> initialized </k>
                  <env> E </env>
                  <id> ID </id>
                  <nrounds> 0 </nrounds>
                  <maxrounds> 1 </maxrounds>
                ...</agent>)
       <nbots> ID:Int => ID +Int 1 </nbots>
       <varcode> VC </varcode>
       <stage> Preprocessing </stage>                 requires N =/=K 1



  syntax KItem ::= "initialized"
  syntax KItem ::= "BR"
  rule <agent>...
        <k> NAgents 1 ; => VC ~> initialized...</k>
        <id> ID </id>
       ...</agent>
       <varcode> VC </varcode>
       <stage> Preprocessing </stage>

  rule <k> initialized => . ...</k>
       <initialized> N => N +Int 1 </initialized> 

  
  rule <nbots> N </nbots>
       <initialized> N </initialized>
       <stage> Preprocessing => Simulation </stage>

  rule 
       <time> T:Int => T +Int 1 </time>
       <state> Environment => Program </state> 
       <maxtime> MT </maxtime>  
       <stage> Simulation </stage> 	requires T <=Int MT

  syntax KItem ::= "loopCode" 


  rule <agent> ... <k> .K => loopCode ~> Code </k><nrounds> N </nrounds> <maxrounds> M </maxrounds> 
         ... </agent>
       <botcode> Code </botcode> 
       <livebots> L:Int => L +Int 1 </livebots>
       <state> Program </state> 
       <stage> Simulation </stage>  requires N <Int M                            
    
  rule <agent>... <k> loopCode => . ... </k> ... </agent>
  // Variable lookup
  rule <agent>...
         <k> X:Id => V ...</k>
         <env>... X |-> L ...</env>
       ...</agent>
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
  rule I1 != I2 => I1 >Int I2 orBool I1 <Int I2
  // Equality
  rule V1:Val == V2:Val => V1 ==K V2

  // Logical Operator
  rule not T => notBool(T)
  rule true and E => E
  rule false and _ => false

  syntax KItem ::= clojure(Exp, Stmts, Stmts)
  rule if (E:Exp) Br S1:Stmts else Br S2:Stmts => clojure(E, S1, S2)
  context clojure(HOLE:Exp, S1,S2)
  rule clojure(true, S1, S2) => S1 
  rule clojure(false, S1, S2) => S2 
 
  syntax KItem ::= "sleep"
  rule events Br Evs:Events => Evs ~> sleep 
  
  rule .Events => .

  syntax KItem ::= closure(Exp, Stmts, Events) 
  rule X:Id() Br pre(E:Exp); eff Br Ss:Stmts  Evs:Events => closure(E, Ss, Evs)

  context closure(HOLE:Exp, Ss, Evs)

  rule closure(true, Ss, Evs) => Ss         
  rule closure(false, Ss, Evs) => Evs


  
 rule <agent>...
         <k> sleep =>. ...</k>
         <id> ID </id><nrounds> M => M +Int 1 </nrounds> 
       ...</agent>
       <livebots> N => N -Int 1 </livebots>

  rule <livebots> 0 </livebots>
       <time> T </time>
       <state> Program => Environment </state>   


  rule <agent> ... <nrounds> N => 0 </nrounds> ... </agent> 
       <state> Environment </state>				requires N>Int 0 
				
endmodule
