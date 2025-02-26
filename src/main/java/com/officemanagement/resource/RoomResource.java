package com.officemanagement.resource;

import com.officemanagement.model.OfficeRoom;
import com.officemanagement.model.Seat;
import com.officemanagement.model.Floor;
import com.officemanagement.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {
    private final SessionFactory sessionFactory;

    public RoomResource() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    @POST
    public Response createRoom(OfficeRoom room) {
        // Validate input
        if (room == null || room.getName() == null || room.getName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Room name is required")
                .build();
        }

        if (room.getRoomNumber() == null || room.getRoomNumber().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Room number is required")
                .build();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            // Validate that floor is provided
            if (room.getFloor() == null || room.getFloor().getId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Floor reference is required")
                    .build();
            }
            
            // Load the referenced floor
            Floor floor = session.get(Floor.class, room.getFloor().getId());
            if (floor == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Referenced floor does not exist")
                    .build();
            }

            // Check for duplicate room number in the same floor
            Long count = session.createQuery(
                "SELECT COUNT(r) FROM OfficeRoom r WHERE r.floor.id = :floorId AND r.roomNumber = :roomNumber", Long.class)
                .setParameter("floorId", floor.getId())
                .setParameter("roomNumber", room.getRoomNumber())
                .uniqueResult();

            if (count > 0) {
                return Response.status(Response.Status.CONFLICT)
                    .entity("A room with number " + room.getRoomNumber() + " already exists on this floor")
                    .build();
            }

            room.setFloor(floor);
            room.setCreatedAt(LocalDateTime.now());
            
            session.save(room);
            session.getTransaction().commit();
            
            return Response.status(Response.Status.CREATED)
                .entity(room)
                .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getRoom(@PathParam("id") Long id) {
        try (Session session = sessionFactory.openSession()) {
            OfficeRoom room = session.createQuery(
                "select distinct r from OfficeRoom r " +
                "left join fetch r.seats s " +
                "left join fetch s.employees " +
                "where r.id = :id", OfficeRoom.class)
                .setParameter("id", id)
                .uniqueResult();
                
            if (room == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            
            return Response.ok(room).build();
        }
    }

    @GET
    @Path("/{id}/seats")
    public Response getRoomSeats(@PathParam("id") Long id) {
        try (Session session = sessionFactory.openSession()) {
            OfficeRoom room = session.createQuery(
                "select distinct r from OfficeRoom r " +
                "left join fetch r.seats " +
                "where r.id = :id", OfficeRoom.class)
                .setParameter("id", id)
                .uniqueResult();
                
            if (room == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            
            List<Seat> seats = room.getSeats();
            return Response.ok(seats).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateRoom(@PathParam("id") Long id, OfficeRoom room) {
        // Validate input
        if (room == null || room.getName() == null || room.getName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Room name is required")
                .build();
        }

        if (room.getRoomNumber() == null || room.getRoomNumber().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Room number is required")
                .build();
        }

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            // Check if room exists
            OfficeRoom existingRoom = session.get(OfficeRoom.class, id);
            if (existingRoom == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Room not found")
                    .build();
            }
            
            // Validate that floor is provided
            if (room.getFloor() == null || room.getFloor().getId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Floor reference is required")
                    .build();
            }
            
            // Load the referenced floor
            Floor floor = session.get(Floor.class, room.getFloor().getId());
            if (floor == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Referenced floor does not exist")
                    .build();
            }

            // Check for duplicate room number in the same floor (excluding current room)
            Long count = session.createQuery(
                "SELECT COUNT(r) FROM OfficeRoom r WHERE r.floor.id = :floorId AND r.roomNumber = :roomNumber AND r.id != :roomId", Long.class)
                .setParameter("floorId", floor.getId())
                .setParameter("roomNumber", room.getRoomNumber())
                .setParameter("roomId", id)
                .uniqueResult();

            if (count > 0) {
                return Response.status(Response.Status.CONFLICT)
                    .entity("A room with number " + room.getRoomNumber() + " already exists on this floor")
                    .build();
            }

            existingRoom.setName(room.getName());
            existingRoom.setRoomNumber(room.getRoomNumber());
            existingRoom.setFloor(floor);
            
            session.update(existingRoom);
            session.getTransaction().commit();
            
            return Response.ok(existingRoom).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteRoom(@PathParam("id") Long id) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            // Check if room exists
            OfficeRoom room = session.get(OfficeRoom.class, id);
            if (room == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Room not found")
                    .build();
            }

            // Check if room has seats
            Long seatCount = session.createQuery(
                "SELECT COUNT(s) FROM Seat s WHERE s.room.id = :roomId", Long.class)
                .setParameter("roomId", id)
                .uniqueResult();

            if (seatCount > 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Cannot delete room that has seats")
                    .build();
            }
            
            session.delete(room);
            session.getTransaction().commit();
            
            return Response.noContent().build();
        }
    }
} 