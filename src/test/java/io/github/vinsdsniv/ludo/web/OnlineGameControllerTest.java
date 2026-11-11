package io.github.vinsdsniv.ludo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vinsdsniv.ludo.application.OnlineGames;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OnlineGameControllerTest {
    private static final String ROOMS = "/api/online-games";
    private static final String TOKEN = "X-Player-Token";

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new OnlineGameController(new OnlineGames()))
            .setControllerAdvice(new GameErrors())
            .build();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void inviteReservesOneSeatAndSameTokenCanRetryJoining() throws Exception {
        JsonNode host = create();
        String room = room(host);
        String guest = UUID.randomUUID().toString();
        join(room, guest).andExpect(status().isOk())
                .andExpect(jsonPath("$.game.player").value(1))
                .andExpect(jsonPath("$.game.ready").value(true))
                .andExpect(jsonPath("$.token").value(guest));
        join(room, guest).andExpect(status().isOk())
                .andExpect(jsonPath("$.game.player").value(1));
        join(room, UUID.randomUUID().toString()).andExpect(status().isConflict());

        mvc.perform(get(room).header(TOKEN, token(host)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.player").value(0))
                .andExpect(jsonPath("$.token").doesNotExist());
        mvc.perform(get(room).header(TOKEN, UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get(room)).andExpect(status().isBadRequest());
    }

    @Test
    void onlyCurrentPlayerCanPlayAndBothClientsSeeTheSameRevision() throws Exception {
        JsonNode host = create();
        String room = room(host);
        String guest = UUID.randomUUID().toString();

        play(room, "/roll", token(host), "{\"revision\":0}").andExpect(status().isConflict());
        join(room, guest).andExpect(status().isOk());
        play(room, "/roll", guest, "{\"revision\":0}").andExpect(status().isConflict());
        play(room, "/move", guest, "{\"token\":0,\"revision\":0}").andExpect(status().isConflict());
        play(room, "/roll", UUID.randomUUID().toString(), "{\"revision\":0}")
                .andExpect(status().isForbidden());

        JsonNode rolled = body(play(room, "/roll", token(host), "{\"revision\":0}")
                .andExpect(status().isOk()));
        JsonNode guestView = body(mvc.perform(get(room).header(TOKEN, guest)).andExpect(status().isOk()));
        assertThat(rolled.get("state")).isEqualTo(guestView.get("state"));
        assertThat(guestView.at("/state/revision").asLong()).isEqualTo(1);
        play(room, "/roll", token(host), "{\"revision\":0}").andExpect(status().isConflict());
    }

    @Test
    void unknownInviteReturnsNotFound() throws Exception {
        join(ROOMS + "/" + UUID.randomUUID(), UUID.randomUUID().toString())
                .andExpect(status().isNotFound());
    }

    private JsonNode create() throws Exception {
        return body(mvc.perform(post(ROOMS)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.game.ready").value(false))
                .andExpect(jsonPath("$.game.player").value(0)));
    }

    private ResultActions join(String room, String token) throws Exception {
        return mvc.perform(post(room + "/join").header(TOKEN, token));
    }

    private ResultActions play(String room, String action, String token, String body) throws Exception {
        return mvc.perform(post(room + action).header(TOKEN, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsByteArray());
    }

    private static String room(JsonNode host) {
        return ROOMS + "/" + host.at("/game/id").asText();
    }

    private static String token(JsonNode host) {
        return host.get("token").asText();
    }
}
