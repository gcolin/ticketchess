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
        if (nrffe.startsWith("@")) {
            CustomPlayer p = customPlayerService.find(Integer.parseInt(nrffe.substring(1)));
            if (p != null) {
                customPlayerService.detach(p);
            }
            if (eventType != null) {
                try {
                    Player player = luceneDb.searchJoueur(p.getLicence());
                    if (player != null) {
                        return player;
                    }
                } catch (ParseException | IOException e) {
                    LoggerFactory.getLogger(this.getClass().getName()).debug("cannot find player " + nrffe, e);
                }
            }
            return p;
        } else {
            try {
                return luceneDb.searchJoueur(nrffe);
            } catch (ParseException | IOException e) {
                LoggerFactory.getLogger(this.getClass().getName()).error("cannot find player " + nrffe, e);
                return null;
            }
        }
    }
}
