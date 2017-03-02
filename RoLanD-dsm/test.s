MW {
  int NumAdded = 0; 
  int CurrentTotal = 0;
}

boolean isFinal = false;
boolean Added = false;
int FinalSum = 0;
 
Step() {
  adding() {
    pre(Added == false);
    eff {
      atomic{
        Added = true;
        CurrentTotal = CurrentTotal +robotIndex;
	NumAdded = NumAdded+1;
      }
    }
  }

}

Sim 2 {50, 100} {5, 10} 200;
