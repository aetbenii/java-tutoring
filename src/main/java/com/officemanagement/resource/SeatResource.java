package com.officemanagement.resource;

import com.officemanagement.model.Seat;
import com.officemanagement.model.OfficeRoom;
import com.officemanagement.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;

@Path("/seats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SeatResource {
    private final SessionFactory sessionFactory;

    public SeatResource() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @GET
    @Path("/{id}")
    public Response getSeat(@PathParam("id") Long id) {
        try (Session session = sessionFactory.openSession()) {
            Seat seat = session.get(Seat.class, id);
                
            if (seat == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            
            return Response.ok(seat).build();
        }
    }

    @POST
    public Response createSeat(Seat seat) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            // Validate that room is provided
            if (seat.getRoom() == null || seat.getRoom().getId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Room reference is required")
                    .build();
            }
            
            // Load the referenced room
            OfficeRoom room = session.get(OfficeRoom.class, seat.getRoom().getId());
            if (room == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Referenced room does not exist")
                    .build();
            }

            // Check for duplicate seat number in the same room
            String hql = "FROM Seat s WHERE s.room.id = :roomId AND s.seatNumber = :seatNumber";
            Long count = session.createQuery(hql, Seat.class)
                .setParameter("roomId", room.getId())
                .setParameter("seatNumber", seat.getSeatNumber())
                .stream()
                .count();

            if (count > 0) {
                return Response.status(Response.Status.CONFLICT)
                    .entity("A seat with number " + seat.getSeatNumber() + " already exists in this room")
                    .build();
            }
            
            // Set the room and creation timestamp
            seat.setRoom(room);
            seat.setCreatedAt(LocalDateTime.now());
            
            // Save the seat
            session.save(seat);
            session.getTransaction().commit();
            
            // Refresh the seat to get the generated ID
            session.refresh(seat);
            
            return Response.status(Response.Status.CREATED)
                .entity(seat)
                .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateSeat(@PathParam("id") Long id, Seat updatedSeat) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            // Check if seat exists
            Seat existingSeat = session.get(Seat.class, id);
            if (existingSeat == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Seat not found")
                    .build();
            }
            
            // Validate that room is provided
            if (updatedSeat.getRoom() == null || updatedSeat.getRoom().getId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Room reference is required")
                    .build();
            }
            
            // Load the referenced room
            OfficeRoom room = session.get(OfficeRoom.class, updatedSeat.getRoom().getId());
            if (room == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Referenced room does not exist")
                    .build();
            }

            // Check for duplicate seat number in the same room (excluding current seat)
            String hql = "FROM Seat s WHERE s.room.id = :roomId AND s.seatNumber = :seatNumber AND s.id != :seatId";
            Long count = session.createQuery(hql, Seat.class)
                .setParameter("roomId", room.getId())
                .setParameter("seatNumber", updatedSeat.getSeatNumber())
                .setParameter("seatId", id)
                .stream()
                .count();

            if (count > 0) {
                return Response.status(Response.Status.CONFLICT)
                    .entity("A seat with number " + updatedSeat.getSeatNumber() + " already exists in this room")
                    .build();
            }
            
            // Update the seat properties
            existingSeat.setSeatNumber(updatedSeat.getSeatNumber());
            existingSeat.setRoom(room);
            
            // Save the changes
            session.update(existingSeat);
            session.getTransaction().commit();
            
            return Response.ok(existingSeat).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteSeat(@PathParam("id") Long id) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            // Check if seat exists
            Seat seat = session.get(Seat.class, id);
            if (seat == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Seat not found")
                    .build();
            }
            
            // Check if seat is assigned to an employee
            if (seat.getEmployee() != null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Cannot delete seat that is assigned to an employee")
                    .build();
            }
            
            // Delete the seat
            session.delete(seat);
            session.getTransaction().commit();
            
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }
} 