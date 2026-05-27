package io.cloudshift.monolith.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomRepository repository;

    public RoomController(RoomRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<RoomView> list() {
        return repository.findAll().stream().map(RoomView::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomView create(@RequestBody CreateRoom request) {
        Room saved = repository.save(new Room(request.name(), request.capacity()));
        return RoomView.of(saved);
    }

    public record CreateRoom(@NotBlank String name, @Min(1) int capacity) {}

    public record RoomView(Long id, String name, int capacity) {
        static RoomView of(Room room) {
            return new RoomView(room.getId(), room.getName(), room.getCapacity());
        }
    }
}
