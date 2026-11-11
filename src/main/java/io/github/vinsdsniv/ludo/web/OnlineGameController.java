package io.github.vinsdsniv.ludo.web;

import io.github.vinsdsniv.ludo.application.OnlineGames;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/online-games")
public class OnlineGameController {
    private static final String TOKEN = "X-Player-Token";
    private final OnlineGames games;

    public OnlineGameController(OnlineGames games) {
        this.games = games;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OnlineGames.Ticket create(@RequestBody(required = false) Profile profile) {
        return games.create(profile == null ? null : profile.nickname());
    }

    @PostMapping("/{id}/join")
    public OnlineGames.Ticket join(@PathVariable UUID id, @RequestHeader(TOKEN) UUID token,
                                   @RequestBody(required = false) Profile profile) {
        return games.join(id, GameErrors.required(token), profile == null ? null : profile.nickname());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OnlineGames.View> read(@PathVariable UUID id, @RequestHeader(TOKEN) UUID token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(games.read(id, token));
    }

    @PostMapping("/{id}/roll")
    public OnlineGames.View roll(@PathVariable UUID id, @RequestHeader(TOKEN) UUID token,
                                 @RequestBody GameController.Roll request) {
        return games.roll(id, GameErrors.required(token), GameErrors.required(request.revision()));
    }

    @PostMapping("/{id}/move")
    public OnlineGames.View move(@PathVariable UUID id, @RequestHeader(TOKEN) UUID token,
                                 @RequestBody GameController.Move request) {
        return games.move(id, GameErrors.required(token), GameErrors.required(request.token()),
                GameErrors.required(request.revision()));
    }

    @PostMapping("/{id}/resign")
    public OnlineGames.View resign(@PathVariable UUID id, @RequestHeader(TOKEN) UUID token) {
        return games.resign(id, GameErrors.required(token));
    }

    public record Profile(String nickname) {}

    @ExceptionHandler(OnlineGames.AccessDenied.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> forbidden(OnlineGames.AccessDenied error) {
        return Map.of("message", error.getMessage());
    }
}
