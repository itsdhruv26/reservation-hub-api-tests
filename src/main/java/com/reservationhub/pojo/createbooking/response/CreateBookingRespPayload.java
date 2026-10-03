package com.reservationhub.pojo.createbooking.response;

import com.reservationhub.pojo.booking.BookingPayload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response body of POST /booking. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRespPayload {

    private int bookingid;
    private BookingPayload booking;
}
