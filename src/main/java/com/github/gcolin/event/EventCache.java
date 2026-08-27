package com.github.gcolin.event;

import com.github.gcolin.event.Event;
import com.github.gcolin.player.DisplayPlayer;
import java.io.Serializable;
import java.util.List;

public class EventCache implements Serializable {

    private static final long serialVersionUID = 1L;
    public Event event;
    public List<DisplayPlayer> players;
    public List<String> missingPlayerCodes = List.of();
    public String eventInfo = "";
}
