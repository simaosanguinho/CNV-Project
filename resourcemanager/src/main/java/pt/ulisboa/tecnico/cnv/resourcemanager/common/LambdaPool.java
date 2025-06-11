package pt.ulisboa.tecnico.cnv.resourcemanager.common;

public class LambdaPool {
    
    private Instance ctfLambda;
    private Instance golLambda;
    private Instance fifteenPuzzleLambda;

    public LambdaPool(String ctfLambda, String golLambda, String fifteenPuzzleLambda) {
        this.ctfLambda = new Instance(ctfLambda);
        this.golLambda = new Instance(golLambda);
        this.fifteenPuzzleLambda = new Instance(fifteenPuzzleLambda);
    }
    public Instance getCtfLambda() {
        return ctfLambda;
    }
    public Instance getGolLambda() {
        return golLambda;
    }
    public Instance getFifteenPuzzleLambda() {
        return fifteenPuzzleLambda;
    }
}
