package lld.parking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnparkRequest {
    @NotBlank(message = "Ticket ID is required")
    private String ticketId;
}
