package com.github.gcolin.desk;

import java.util.ArrayList;
import java.util.List;

public class EventDeskEventDto {

    private int id;
    private String name;
    private boolean free;
    private List<EventDeskPlayerDto> players = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public List<EventDeskPlayerDto> getPlayers() {
        return players;
    }

    public void setPlayers(List<EventDeskPlayerDto> players) {
        this.players = players == null ? new ArrayList<>() : players;
    }
}
