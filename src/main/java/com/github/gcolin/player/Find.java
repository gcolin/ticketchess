package com.github.gcolin.player;

import com.github.gcolin.event.EventType;
import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.LoggerFactory;

public class Find {

    private LuceneDb luceneDb;
    private CustomPlayerDao customPlayerService;

    public void setLuceneDb(LuceneDb luceneDb) {
        this.luceneDb = luceneDb;
    }

    public void setCustomPlayerDao(CustomPlayerDao customPlayerService) {
        this.customPlayerService = customPlayerService;
    }

    public IPlayer player(String nrffe, EventType eventType) {
        if (nrffe == null || nrffe.isBlank()) {
            return null;
        }
        String ref = nrffe.trim();
        if (ref.startsWith("@")) {
            return resolveCustomPlayer(ref, eventType);
        }
        try {
            return luceneDb.searchJoueur(ref);
        } catch (ParseException | IOException e) {
            LoggerFactory.getLogger(getClass()).error("cannot find player " + ref, e);
            return null;
        }
    }

    private IPlayer resolveCustomPlayer(String ref, EventType eventType) {
        CustomPlayer custom;
        try {
            custom = customPlayerService.find(Integer.parseInt(ref.substring(1)));
        } catch (NumberFormatException e) {
            LoggerFactory.getLogger(getClass()).error("invalid custom player ref " + ref, e);
            return null;
        }
        if (custom == null) {
            return null;
        }
        customPlayerService.detach(custom);
        String licence = custom.getLicence();
        if (eventType != null && licence != null && !licence.isBlank()) {
            try {
                Player fromFfe = luceneDb.searchJoueur(licence);
                if (fromFfe != null) {
                    return fromFfe;
                }
            } catch (ParseException | IOException e) {
                LoggerFactory.getLogger(getClass()).debug("cannot enrich custom player " + ref, e);
            }
        }
        return custom;
    }
}
