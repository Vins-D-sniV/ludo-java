package io.github.vinsdsniv.ludo.web;

import io.github.vinsdsniv.ludo.application.Games;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final Games games;

    public GameController(Games games) {
        this.games = games;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Games.Session create(@RequestBody NewGame request) {
        return games.create(GameErrors.required(request.players()));
    }

    @GetMapping("/{id}")
    public Games.Session read(@PathVariable UUID id) {
        return games.read(id);
    }

    @PostMapping("/{id}/roll")
    public Games.Session roll(@PathVariable UUID id, @RequestBody Roll request) {
        return games.roll(id, GameErrors.required(request.revision()));
    }

    @PostMapping("/{id}/move")
    public Games.Session move(@PathVariable UUID id, @RequestBody Move request) {
        return games.move(id, GameErrors.required(request.token()), GameErrors.required(request.revision()));
    }

    public record NewGame(Integer players) {}
    public record Roll(Long revision) {}
    public record Move(Integer token, Long revision) {}
}
