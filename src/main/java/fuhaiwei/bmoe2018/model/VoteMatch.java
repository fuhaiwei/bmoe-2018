package fuhaiwei.bmoe2018.model;

import java.util.List;

public class VoteMatch {

    private String title;
    private List<VoteData> voteDatas;
    private List<VoteGroup> voteGroups;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<VoteData> getVoteDatas() {
        return voteDatas;
    }

    public void setVoteDatas(List<VoteData> voteDatas) {
        this.voteDatas = voteDatas;
    }

    public List<VoteGroup> getVoteGroups() {
        return voteGroups;
    }

    public void setVoteGroups(List<VoteGroup> voteGroups) {
        this.voteGroups = voteGroups;
    }

}
