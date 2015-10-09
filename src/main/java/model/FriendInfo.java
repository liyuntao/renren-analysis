package model;

import java.io.Serializable;
import java.util.Objects;

public class FriendInfo implements Serializable {
    private String uid;
    private String school;
    private String name;
    private String link;

    public FriendInfo(String uid, String school, String name, String link) {
        this.uid = uid;
        this.school = school;
        this.name = name;
        this.link = link;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendInfo that = (FriendInfo) o;
        return uid.equals(that.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uid);
    }

    @Override
    public String toString() {
        return "Friend(" + uid + ", " + school + ", " + name + ", " + link + ")";
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }
}

