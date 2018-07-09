package fuhaiwei.bmoe2018.model;

import java.util.Objects;

public class UnionVote {

    private static UnionVote END = new UnionVote();

    private Character character;
    private UnionVote next;

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public UnionVote getNext() {
        return next;
    }

    public void setNext(UnionVote next) {
        this.next = next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UnionVote other = (UnionVote) o;
        if (character != (other).character) {
            return false;
        }
        return next.equals((other).next);
    }

    @Override
    public int hashCode() {
        return Objects.hash(character, next);
    }

}
