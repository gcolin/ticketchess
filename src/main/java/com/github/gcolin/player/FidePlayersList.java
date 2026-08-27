package com.github.gcolin.player;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "playerslist")
@XmlAccessorType(XmlAccessType.FIELD)
public class FidePlayersList {

    @XmlElement(name = "player")
    private List<FidePlayer> players;

    public List<FidePlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<FidePlayer> players) {
        this.players = players;
    }
}
