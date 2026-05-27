package io.cloudshift.monolith.reservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationRepository repository;

    public ReservationController(ReservationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ReservationView> list() {
        return repository.findAll().stream().map(ReservationView::of).toList();
    }

    @GetMapping("/{id}")
    public ReservationView get(@PathVariable Long id) {
        return repository
                .findById(id)
                .map(ReservationView::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationView create(@Valid @RequestBody CreateReservation request) {
        Reservation saved =
                repository.save(
                        new Reservation(
                                request.roomId(),
                                request.guestName(),
                                request.checkIn(),
                                request.checkOut()));
        return ReservationView.of(saved);
    }

    public record CreateReservation(
            @NotNull Long roomId,
            @NotBlank String guestName,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut) {}

    public record ReservationView(
            Long id, Long roomId, String guestName, LocalDate checkIn, LocalDate checkOut) {
        static ReservationView of(Reservation reservation) {
            return new ReservationView(
                    reservation.getId(),
                    reservation.getRoomId(),
                    reservation.getGuestName(),
                    reservation.getCheckIn(),
                    reservation.getCheckOut());
        }
    }
}
