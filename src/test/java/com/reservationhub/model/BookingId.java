package com.reservationhub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One entry of the GET /booking response, which is a JSON array of {@code {"bookingid": 1}} objects. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingId {

    private int bookingid;
}
