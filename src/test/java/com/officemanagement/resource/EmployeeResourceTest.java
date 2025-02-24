package com.officemanagement.resource;

import com.officemanagement.model.Employee;
import com.officemanagement.model.Floor;
import com.officemanagement.model.OfficeRoom;
import com.officemanagement.model.Seat;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class EmployeeResourceTest extends BaseResourceTest {

    @Test
    public void testCreateEmployee() {
        Employee employee = new Employee();
        employee.setFullName("John Doe");
        employee.setOccupation("Software Engineer");
        employee.setCreatedAt(LocalDateTime.now());

        given()
            .contentType(ContentType.JSON)
            .body(employee)
        .when()
            .post(getApiPath("/employees"))
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .body("id", notNullValue())
            .body("fullName", equalTo("John Doe"))
            .body("occupation", equalTo("Software Engineer"));
    }

    @Test
    public void testGetEmployee() {
        // First create an employee directly in the database
        Employee employee = new Employee();
        employee.setFullName("Jane Smith");
        employee.setOccupation("Product Manager");
        employee.setCreatedAt(LocalDateTime.now());
        session.save(employee);
        commitAndStartNewTransaction();

        // Then get the employee through the API
        given()
        .when()
            .get(getApiPath("/employees/" + employee.getId()))
        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("fullName", equalTo("Jane Smith"))
            .body("occupation", equalTo("Product Manager"));
    }

    @Test
    public void testGetNonExistentEmployee() {
        given()
        .when()
            .get(getApiPath("/employees/999999"))
        .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testSearchEmployees() {
        // Create test employees directly in the database
        createTestEmployee("John Developer", "Software Engineer");
        createTestEmployee("Jane Designer", "UI Designer");
        createTestEmployee("Bob Manager", "Product Manager");
        commitAndStartNewTransaction();
        flushAndClear();

        // Search by name
        given()
            .queryParam("search", "John")
            .queryParam("page", "0")
            .queryParam("size", "10")
        .when()
            .get(getApiPath("/employees/search"))
        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("content", hasSize(greaterThanOrEqualTo(1)))
            .body("content.find { it.fullName == 'John Developer' }.occupation", 
                  equalTo("Software Engineer"));

        // Search by occupation
        given()
            .queryParam("search", "Engineer")
            .queryParam("page", "0")
            .queryParam("size", "10")
        .when()
            .get(getApiPath("/employees/search"))
        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("content", hasSize(greaterThanOrEqualTo(1)))
            .body("content.find { it.occupation == 'Software Engineer' }", notNullValue());
    }

    @Test
    public void testAssignAndUnassignSeat() {
        // Create test data directly in the database
        Employee employee = new Employee();
        employee.setFullName("Test Employee");
        employee.setOccupation("Tester");
        employee.setCreatedAt(LocalDateTime.now());
        session.save(employee);

        // Create a floor first
        Floor floor = new Floor();
        floor.setName("Test Floor");
        floor.setFloorNumber(1);
        floor.setCreatedAt(LocalDateTime.now());
        session.save(floor);

        // Create a room
        OfficeRoom room = new OfficeRoom();
        room.setName("Test Room");
        room.setRoomNumber("101");
        room.setFloor(floor);
        room.setCreatedAt(LocalDateTime.now());
        session.save(room);

        // Create a seat with room reference
        Seat seat = new Seat();
        seat.setSeatNumber("Test Seat");
        seat.setCreatedAt(LocalDateTime.now());
        seat.setRoom(room);
        session.save(seat);
        
        commitAndStartNewTransaction();
        flushAndClear();

        // Assign seat to employee
        given()
        .when()
            .put(getApiPath("/employees/" + employee.getId() + "/assign-seat/" + seat.getId()))
        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("seats", hasSize(1))
            .body("seats[0].id", equalTo(seat.getId().intValue()));

        // Unassign seat from employee
        given()
        .when()
            .delete(getApiPath("/employees/" + employee.getId() + "/unassign-seat/" + seat.getId()))
        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("seats", hasSize(0));
    }

    @Test
    public void testCreateEmployeeWithInvalidData() {
        // Test with empty employee
        Employee emptyEmployee = new Employee();
        emptyEmployee.setCreatedAt(LocalDateTime.now());

        given()
            .contentType(ContentType.JSON)
            .body(emptyEmployee)
        .when()
            .post(getApiPath("/employees"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with null values
        Employee nullEmployee = new Employee();
        nullEmployee.setFullName(null);
        nullEmployee.setOccupation(null);
        nullEmployee.setCreatedAt(LocalDateTime.now());

        given()
            .contentType(ContentType.JSON)
            .body(nullEmployee)
        .when()
            .post(getApiPath("/employees"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with invalid content type
        given()
            .contentType(ContentType.TEXT)
            .body("Invalid data")
        .when()
            .post(getApiPath("/employees"))
        .then()
            .statusCode(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void testAssignSeatWithInvalidIds() {
        // Test with non-existent employee ID
        given()
        .when()
            .put(getApiPath("/employees/99999/assign-seat/1"))
        .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());

        // Create an employee but use invalid seat ID
        Employee employee = new Employee();
        employee.setFullName("Test Employee");
        employee.setOccupation("Tester");
        employee.setCreatedAt(LocalDateTime.now());
        session.save(employee);
        commitAndStartNewTransaction();

        given()
        .when()
            .put(getApiPath("/employees/" + employee.getId() + "/assign-seat/99999"))
        .then()
            .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testSearchEmployeesWithInvalidParameters() {
        // Test with negative page number
        given()
            .queryParam("search", "John")
            .queryParam("page", "-1")
            .queryParam("size", "10")
        .when()
            .get(getApiPath("/employees/search"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with negative page size
        given()
            .queryParam("search", "John")
            .queryParam("page", "0")
            .queryParam("size", "-1")
        .when()
            .get(getApiPath("/employees/search"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Test with extremely large page size
        given()
            .queryParam("search", "John")
            .queryParam("page", "0")
            .queryParam("size", "1000000")
        .when()
            .get(getApiPath("/employees/search"))
        .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private void createTestEmployee(String fullName, String occupation) {
        Employee employee = new Employee();
        employee.setFullName(fullName);
        employee.setOccupation(occupation);
        employee.setCreatedAt(LocalDateTime.now());
        session.save(employee);
        session.flush();
    }
} 