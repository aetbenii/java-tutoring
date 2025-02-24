package com.officemanagement.resource;

import com.officemanagement.model.Employee;
import com.officemanagement.model.Seat;
import com.officemanagement.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;

// Add static inner class for pagination response
class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;

    public PageResponse(List<T> content, long totalElements, int currentPage, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
        this.size = size;
        this.totalPages = (int) Math.ceil(totalElements / (double) size);
    }

    // Getters and setters
    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}

@Path("/employees")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmployeeResource {
    private final SessionFactory sessionFactory;

    public EmployeeResource() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @GET
    @Path("/{id}")
    public Response getEmployee(@PathParam("id") Long id) {
        try (Session session = sessionFactory.openSession()) {
            Employee employee = session.createQuery(
                "select distinct e from Employee e " +
                "left join fetch e.seats s " +
                "left join fetch s.room r " +
                "where e.id = :id", 
                Employee.class)
                .setParameter("id", id)
                .uniqueResult();
            
            if (employee == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(employee).build();
        }
    }

    @GET
    @Path("/{id}/seats")
    public Response getEmployeeSeats(@PathParam("id") Long id) {
        try (Session session = sessionFactory.openSession()) {
            Employee employee = session.createQuery(
                "select distinct e from Employee e " +
                "left join fetch e.seats s " +
                "left join fetch s.room r " +
                "where e.id = :id", 
                Employee.class)
                .setParameter("id", id)
                .uniqueResult();
            
            if (employee == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(employee.getSeats()).build();
        }
    }

    @POST
    public Response createEmployee(Employee employee) {
        // Validate input
        if (employee == null || employee.getFullName() == null || employee.getFullName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Employee full name is required")
                .build();
        }

        if (employee.getOccupation() == null || employee.getOccupation().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Employee occupation is required")
                .build();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            employee.setCreatedAt(LocalDateTime.now());
            session.save(employee);
            session.getTransaction().commit();
            return Response.status(Response.Status.CREATED).entity(employee).build();
        }
    }

    @PUT
    @Path("/{id}/assign-seat/{seatId}")
    public Response assignSeat(@PathParam("id") Long employeeId, @PathParam("seatId") Long seatId) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            Employee employee = session.get(Employee.class, employeeId);
            if (employee == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Employee not found").build();
            }

            Seat seat = session.get(Seat.class, seatId);
            if (seat == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Seat not found").build();
            }

            // Check if seat is already occupied
            if (seat.getEmployee() != null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Seat is already occupied").build();
            }

            // Add seat to employee's seats
            employee.addSeat(seat);
            
            // Update both entities
            session.update(seat);
            session.update(employee);
            
            session.getTransaction().commit();
            
            return Response.ok(employee).build();
        }
    }

    @DELETE
    @Path("/{employeeId}/unassign-seat/{seatId}")
    public Response unassignSeat(@PathParam("employeeId") Long employeeId, @PathParam("seatId") Long seatId) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            Employee employee = session.get(Employee.class, employeeId);
            if (employee == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Employee not found").build();
            }

            Seat seat = session.get(Seat.class, seatId);
            if (seat == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Seat not found").build();
            }

            // Check if this seat belongs to the employee
            if (seat.getEmployee() == null || !seat.getEmployee().getId().equals(employeeId)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("This seat is not assigned to the employee").build();
            }

            // Just set the employee reference to null instead of removing the seat
            seat.setEmployee(null);
            session.update(seat);
            
            session.getTransaction().commit();
            
            // Refresh the employee to get the updated state
            session.refresh(employee);
            
            return Response.ok(employee).build();
        }
    }

    @GET
    @Path("/search")
    public Response searchEmployees(
            @QueryParam("search") @DefaultValue("") String searchTerm,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        
        // Validate pagination parameters
        if (page < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Page number cannot be negative")
                .build();
        }

        if (size <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Page size must be positive")
                .build();
        }

        // Set a reasonable maximum page size to prevent performance issues
        final int MAX_PAGE_SIZE = 100;
        if (size > MAX_PAGE_SIZE) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Page size cannot exceed " + MAX_PAGE_SIZE)
                .build();
        }

        try (Session session = sessionFactory.openSession()) {
            // Create the base query for total count
            String countQuery = "select count(distinct e) from Employee e " +
                    "where lower(e.fullName) like lower(:searchTerm) " +
                    "or lower(e.occupation) like lower(:searchTerm)";
            
            Long totalElements = session.createQuery(countQuery, Long.class)
                    .setParameter("searchTerm", "%" + searchTerm + "%")
                    .uniqueResult();

            // Create the main query with pagination
            String query = "select distinct e from Employee e " +
                    "left join fetch e.seats s " +
                    "left join fetch s.room r " +
                    "where lower(e.fullName) like lower(:searchTerm) " +
                    "or lower(e.occupation) like lower(:searchTerm)";

            List<Employee> employees = session.createQuery(query, Employee.class)
                    .setParameter("searchTerm", "%" + searchTerm + "%")
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .list();

            PageResponse<Employee> pageResponse = new PageResponse<>(
                employees, totalElements, page, size
            );

            return Response.ok(pageResponse).build();
        }
    }
}
