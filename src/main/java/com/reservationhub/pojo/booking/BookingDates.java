package com.reservationhub.pojo.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Dates are kept as strings so tests can send and observe malformed values verbatim.
 * Either field may be null, which lets the same type serve as the nested part of a PATCH.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingDates {

    private String checkin;
    private String checkout;

    public static BookingDates of(LocalDate checkin, LocalDate checkout) {
        return new BookingDates(checkin.toString(), checkout.toString());
    }
}
