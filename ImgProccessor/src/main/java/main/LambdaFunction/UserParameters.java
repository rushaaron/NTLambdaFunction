package main.LambdaFunction;

public class UserParameters {

    private final int redBranch;
    private final int redCellBranch;
    private final int xOffset;
    private final int yOffset;
    private final int stemToIgnore;

    public UserParameters(int redBranch, int redCellBranch, int xOffset, int yOffset, String stemToIgnore) {
        this.redBranch = redBranch;
        this.redCellBranch = redCellBranch;
        this.xOffset = xOffset;
        this.yOffset = yOffset;

        switch(stemToIgnore) {
            case "N":
                this.stemToIgnore = 3;
                break;
            case "E":
                this.stemToIgnore = 2;
                break;
            case "S":
                this.stemToIgnore = 4;
                break;
            case "W":
                this.stemToIgnore = 1;
                break;
            default:
                this.stemToIgnore = 0;
                break;
        }
    }

    //
    // Need to add a validation for values
    //
//    private void validateValues() {
//        if (xOffsetFromEye > 100)
//            xOffsetFromEye = 100;
//        else if (xOffsetFromEye < 5)
//            xOffsetFromEye = 5;
//        if (yOffsetFromEye > 100)
//            yOffsetFromEye = 100;
//        else if (yOffsetFromEye < 5)
//            yOffsetFromEye = 5;
//        if (startOfBranchRed > 254)
//            startOfBranchRed = 254;
//        else if (startOfBranchRed < 5)
//            startOfBranchRed = 5;
//        if (red > 254)
//            red = 254;
//        else if (red < 5)
//            red = 5;
//    }

    // Getter methods
    public int getRedBranch() {
        return redBranch;
    }

    public int getRedCellBranch() {
        return redCellBranch;
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public int getStemToIgnore() {
        return stemToIgnore;
    }

    public String toString() {
        return ("Starting tracing. X offset From the Eye: " + xOffset + ", Y offset From The Eye: " + yOffset +
                ". Starting Red Branch Shade: " + redBranch + ". Red Cell Branch Shade: " + redCellBranch + ". Stem to ignore: " + stemToIgnore);
    }

}
