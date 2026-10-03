package com.reservationhub.pojo.getbookingids.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One entry of the GET /booking response, which is a JSON array of {@code {"bookingid": 1}} objects. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingIdRespPayload {

    private int bookingid;
}
