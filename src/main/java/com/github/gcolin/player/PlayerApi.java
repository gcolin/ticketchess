package com.github.gcolin.player;

import com.github.gcolin.auth.RequirePermission;
import com.github.gcolin.auth.PermissionCode;
import com.github.gcolin.player.LuceneDb;
import com.github.gcolin.player.Player;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;

@Path("player")
public class PlayerApi {

    @Inject
    private LuceneDb luceneDb;

    @GET
    @Path("{licence}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @RequirePermission(PermissionCode.ADMIN_PANEL)
    public Player getPlayers(@PathParam("licence") String licence) {
        try {
            Player p = luceneDb.searchJoueur(licence);
            System.out.println(p + " >" + licence + "< ");
            return p;
        } catch (ParseException | IOException e) {
            throw new WebApplicationException(e);
        }
    }
}
