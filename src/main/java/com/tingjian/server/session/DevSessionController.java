package com.tingjian.server.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@Profile("dev")
@RequestMapping("/api/dev/sessions")
public class DevSessionController {
    private final SessionService service;

    public DevSessionController(SessionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionRepository.SessionView create(@Valid @RequestBody CreateRequest request) {
        return service.create(request.title());
    }

    @GetMapping
    public List<SessionRepository.SessionView> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public Detail detail(@PathVariable String id) {
        return new Detail(service.get(id), service.messages(id));
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionRepository.MessageView addMessage(@PathVariable String id,
                                                     @Valid @RequestBody AddMessageRequest request) {
        return service.addMessage(id, request.speaker().name(), request.content());
    }

    @PostMapping("/{id}/end")
    public SessionRepository.SessionView end(@PathVariable String id) {
        return service.end(id);
    }

    public record CreateRequest(@Size(max = 80) String title) {}
    public enum Speaker { OTHER, SELF }
    public record AddMessageRequest(@NotNull Speaker speaker,
                                    @NotBlank @Size(max = 2000) String content) {}
    public record Detail(SessionRepository.SessionView session,
                         List<SessionRepository.MessageView> messages) {}
}
