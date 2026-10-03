package com.reservationhub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response body of POST /booking. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatedBooking {

    private int bookingid;
    private Booking booking;
}
