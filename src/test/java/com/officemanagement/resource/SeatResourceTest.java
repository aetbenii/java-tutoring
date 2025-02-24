package com.officemanagement.resource;

import com.officemanagement.model.Floor;
import com.officemanagement.model.OfficeRoom;
import com.officemanagement.model.Seat;
import com.officemanagement.model.Employee;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;


public class SeatResourceTest extends BaseResourceTest {

    @Test
    public void testCreateSeatWithInvalidData() {
        // Test with empty seat
        Seat emptySeat = new Seat();
        emptySeat.setCreatedAt(LocalDateTime.now());

        given()
            .contentType(ContentType.JSON)
            .body(emptySeat)
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with null values
        Seat nullSeat = new Seat();
        nullSeat.setSeatNumber(null);
        nullSeat.setRoom(null);
        nullSeat.setCreatedAt(LocalDateTime.now());

        given()
            .contentType(ContentType.JSON)
            .body(nullSeat)
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with non-existent room
        OfficeRoom nonExistentRoom = new OfficeRoom();
        nonExistentRoom.setId(99999L);

        Seat seatWithInvalidRoom = new Seat();
        seatWithInvalidRoom.setSeatNumber("A1");
        seatWithInvalidRoom.setRoom(nonExistentRoom);
        seatWithInvalidRoom.setCreatedAt(LocalDateTime.now());

        given()
            .contentType(ContentType.JSON)
            .body(seatWithInvalidRoom)
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with invalid content type
        given()
            .contentType(ContentType.TEXT)
            .body("Invalid data")
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void testDuplicateSeatNumber() {
        // Create necessary test data
        Floor floor = new Floor();
        floor.setName("Test Floor");
        floor.setFloorNumber(1);
        floor.setCreatedAt(LocalDateTime.now());
        session.save(floor);

        OfficeRoom room = new OfficeRoom();
        room.setName("Test Room");
        room.setRoomNumber("101");
        room.setFloor(floor);
        room.setCreatedAt(LocalDateTime.now());
        session.save(room);
        
        commitAndStartNewTransaction();

        // Create first seat
        Seat seat1 = new Seat();
        seat1.setSeatNumber("A1");
        seat1.setRoom(room);
        seat1.setCreatedAt(LocalDateTime.now());

        given()
            .contentType(ContentType.JSON)
            .body(seat1)
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode());

        // Try to create another seat with same number in same room
        Seat seat2 = new Seat();
        seat2.setSeatNumber("A1");  // Same seat number
        seat2.setRoom(room);
        seat2.setCreatedAt(LocalDateTime.now());

        given()
            .contentType(ContentType.JSON)
            .body(seat2)
        .when()
            .post(getApiPath("/seats"))
        .then()
            .statusCode(Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testDeleteSeatWithAssignedEmployee() {
        // Create necessary test data
        Floor floor = new Floor();
        floor.setName("Test Floor");
        floor.setFloorNumber(1);
        floor.setCreatedAt(LocalDateTime.now());
        session.save(floor);

        OfficeRoom room = new OfficeRoom();
        room.setName("Test Room");
        room.setRoomNumber("101");
        room.setFloor(floor);
        room.setCreatedAt(LocalDateTime.now());
        session.save(room);

        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setRoom(room);
        seat.setCreatedAt(LocalDateTime.now());
        session.save(seat);

        Employee employee = new Employee();
        employee.setFullName("Test Employee");
        employee.setOccupation("Tester");
        employee.setCreatedAt(LocalDateTime.now());
        session.save(employee);

        // Assign employee to seat
        employee.addSeat(seat);
        session.flush();
        commitAndStartNewTransaction();

        // Attempt to delete seat with assigned employee (should fail)
        given()
        .when()
            .delete(getApiPath("/seats/" + seat.getId()))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testUpdateNonExistentSeat() {
        Seat seat = new Seat();
        seat.setSeatNumber("A1");
        seat.setCreatedAt(LocalDateTime.now());

        given()
            .contentType(ContentType.JSON)
            .body(seat)
        .when()
            .put(getApiPath("/seats/99999"))
        .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }
} 