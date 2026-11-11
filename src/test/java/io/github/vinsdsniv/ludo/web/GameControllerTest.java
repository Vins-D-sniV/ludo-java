package io.github.vinsdsniv.ludo.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vinsdsniv.ludo.application.Games;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GameControllerTest {
    private static final String GAMES = "/api/games";

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new GameController(new Games())).build();

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void createsReadsAndRollsAGameAndRejectsAStaleRequest() throws Exception {
        String created = send(GAMES, "{\"players\":3}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state.players.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        String game = GAMES + "/" + json.readTree(created).get("id").asText();

        mvc.perform(get(game)).andExpect(status().isOk())
                .andExpect(jsonPath("$.state.revision").value(0));
        send(game + "/move", "{\"token\":0,\"revision\":0}")
                .andExpect(status().isConflict());
        send(game + "/roll", "{\"revision\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.lastRoll").isNumber())
                .andExpect(jsonPath("$.state.revision").value(1));
        send(game + "/roll", "{\"revision\":0}").andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidPlayerCountsAndMissingGames() throws Exception {
        send(GAMES, "{\"players\":1}").andExpect(status().isBadRequest());
        send(GAMES, "{}").andExpect(status().isBadRequest());
        mvc.perform(get(GAMES + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    private ResultActions send(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
