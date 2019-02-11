enum GameOption {SameAgain, Menu, Quit }

// TODO ??? can we drop one of nil & zero ???
// TODO ??? is the extra safety worth the extra typing ???
enum TileState {
    zero(" "), one("1"), two("2"), three("3"), four("4"), five("5"), six("6"), seven("7"), eight("8"),
    nil(""), bomb("*"), marked("X"), maybe("?");
    TileState(String s){text=s;}

    //CAREFUL: text (e.g. "*") vs toString() (eg "bomb")
    String text;
}

enum Difficulty {
    Trivial(4, 3),
    Easy(6, 6),
    Medium(10, 20),
    Fun(15, 40),
    Hard(15,90);

    private int size;
    private int mineCount;

    Difficulty(int size, int count) {
        this.size = size;
        mineCount = count;
    }

    @Override
    public String toString() {
        //  e.g. "Easy (6x6 Grid, 6 Mines)"
        return String.format("%s (%dx%d Grid, %d Mines)",
            super.toString(), size, size, mineCount
        );
    }

    public int getSize() {
        return size;
    }
    public int getMineCount() {
        return mineCount;
    }
}

