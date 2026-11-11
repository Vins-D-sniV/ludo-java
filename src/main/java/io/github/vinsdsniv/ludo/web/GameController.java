package io.github.vinsdsniv.ludo.web;

import io.github.vinsdsniv.ludo.application.Games;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
        return games.create(required(request.players()));
    }

    @GetMapping("/{id}")
    public Games.Session read(@PathVariable UUID id) {
        return games.read(id);
    }

    @PostMapping("/{id}/roll")
    public Games.Session roll(@PathVariable UUID id, @RequestBody Roll request) {
        return games.roll(id, required(request.revision()));
    }

    @PostMapping("/{id}/move")
    public Games.Session move(@PathVariable UUID id, @RequestBody Move request) {
        return games.move(id, required(request.token()), required(request.revision()));
    }

    private static <T> T required(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Un champ obligatoire est manquant.");
        }
        return value;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException error) {
        return Map.of("message", error.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(IllegalStateException error) {
        return Map.of("message", error.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> missing(NoSuchElementException error) {
        return Map.of("message", error.getMessage());
    }

    public record NewGame(Integer players) {}
    public record Roll(Long revision) {}
    public record Move(Integer token, Long revision) {}
}
