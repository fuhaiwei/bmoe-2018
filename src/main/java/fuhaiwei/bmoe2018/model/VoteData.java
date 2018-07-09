package fuhaiwei.bmoe2018.model;

import java.util.List;

public class VoteData {

    private VoteType type;
    private int voteIndex;
    private List<Character> characters;

    public VoteData(int voteIndex, VoteType type, List<Character> characters) {
        this.type = type;
        this.voteIndex = voteIndex;
        this.characters = characters;
    }

    public int getVoteIndex() {
        return voteIndex;
    }

    public VoteType getType() {
        return type;
    }

    public List<Character> getCharacters() {
        return characters;
    }

}
