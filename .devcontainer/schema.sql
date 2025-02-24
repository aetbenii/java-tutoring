-- Drop database if exists and recreate it
DROP DATABASE IF EXISTS office_management;
CREATE DATABASE office_management;
\c office_management;

-- Drop tables if they exist (in correct order due to foreign keys)
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS office_rooms;
DROP TABLE IF EXISTS floors;

-- Drop sequences if they exist
DROP SEQUENCE IF EXISTS employee_seq;
DROP SEQUENCE IF EXISTS seat_seq;
DROP SEQUENCE IF EXISTS office_room_seq;
DROP SEQUENCE IF EXISTS floor_seq;

-- Create sequences
CREATE SEQUENCE employee_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seat_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE office_room_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE floor_seq START WITH 1 INCREMENT BY 1;

-- Create tables in correct order (no forward references)
CREATE TABLE floors (
    id BIGINT DEFAULT nextval('floor_seq') PRIMARY KEY,
    floor_number INTEGER NOT NULL,
    name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE office_rooms (
    id BIGINT DEFAULT nextval('office_room_seq') PRIMARY KEY,
    room_number VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    floor_id BIGINT REFERENCES floors(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employees (
    id BIGINT DEFAULT nextval('employee_seq') PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    occupation VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seats (
    id BIGINT DEFAULT nextval('seat_seq') PRIMARY KEY,
    seat_number VARCHAR(255) NOT NULL,
    room_id BIGINT REFERENCES office_rooms(id),
    employee_id BIGINT REFERENCES employees(id), -- add UNIQUE if the employee can only have one seat
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data in correct order
-- 1. First, insert floors
INSERT INTO floors (floor_number, name) VALUES
(1, 'First Floor'),
(2, 'Second Floor'),
(3, 'Third Floor'),
(4, 'Fourth Floor'),
(5, 'Fifth Floor'),
(6, 'Sixth Floor'),
(7, 'Seventh Floor'),
(8, 'Eighth Floor'),
(9, 'Ninth Floor');

-- 2. Then, insert rooms for each floor
DO $$
DECLARE
    floor_record RECORD;
BEGIN
    FOR floor_record IN SELECT id, floor_number FROM floors ORDER BY floor_number
    LOOP
        FOR room_num IN 1..20
        LOOP
            INSERT INTO office_rooms (room_number, name, floor_id)
            VALUES (
                CONCAT(floor_record.floor_number, LPAD(room_num::text, 2, '0')),
                CONCAT('Room ', floor_record.floor_number, LPAD(room_num::text, 2, '0')),
                floor_record.id
            );
        END LOOP;
    END LOOP;
END $$;

-- 3. Insert seats for each room
DO $$
DECLARE
    room_record RECORD;
BEGIN
    FOR room_record IN SELECT id, room_number FROM office_rooms
    LOOP
        FOR seat_num IN 1..4
        LOOP
            INSERT INTO seats (seat_number, room_id)
            VALUES (
                CONCAT(room_record.room_number, '-', LPAD(seat_num::text, 2, '0')),
                room_record.id
            );
        END LOOP;
    END LOOP;
END $$; 


-- 4. Finally some employee data
INSERT INTO employees (full_name, occupation, created_at) VALUES
-- Italian Names
('Marco Rossi', 'Senior Software Architect', CURRENT_TIMESTAMP),
('Giuseppe Conti', 'DevOps Engineer', CURRENT_TIMESTAMP),
('Alessandro Ferrari', 'Security Systems Specialist', CURRENT_TIMESTAMP),
('Sofia Marino', 'Data Privacy Officer', CURRENT_TIMESTAMP),
('Lorenzo Romano', 'Full Stack Developer', CURRENT_TIMESTAMP),
('Valentina Colombo', 'Systems Analyst', CURRENT_TIMESTAMP),
('Luca Ricci', 'Database Administrator', CURRENT_TIMESTAMP),
('Matteo Greco', 'Cloud Infrastructure Engineer', CURRENT_TIMESTAMP),
('Chiara Esposito', 'Scrum Master', CURRENT_TIMESTAMP),
('Andrea Moretti', 'Software Development Team Lead', CURRENT_TIMESTAMP),
('Francesca Barbieri', 'Quality Assurance Engineer', CURRENT_TIMESTAMP),
('Roberto Mancini', 'Network Security Engineer', CURRENT_TIMESTAMP),
('Elena Lombardi', 'Business Analyst', CURRENT_TIMESTAMP),
('Paolo Gallo', 'Backend Developer', CURRENT_TIMESTAMP),
('Isabella Costa', 'Frontend Developer', CURRENT_TIMESTAMP),
('Davide Fontana', 'Infrastructure Architect', CURRENT_TIMESTAMP),
('Giulia Santoro', 'UX/UI Designer', CURRENT_TIMESTAMP),
('Antonio Marini', 'System Integration Specialist', CURRENT_TIMESTAMP),
('Claudia Vitale', 'Information Security Analyst', CURRENT_TIMESTAMP),
('Stefano Leone', 'API Integration Specialist', CURRENT_TIMESTAMP),
('Maria Longo', 'Technical Project Manager', CURRENT_TIMESTAMP),
('Fabio Ferrara', 'DevSecOps Engineer', CURRENT_TIMESTAMP),
('Laura Pellegrini', 'Data Engineer', CURRENT_TIMESTAMP),
('Vincenzo Serra', 'Software Engineer', CURRENT_TIMESTAMP),
('Cristina Palumbo', 'Agile Coach', CURRENT_TIMESTAMP),
('Emilio Valentini', 'Solutions Architect', CURRENT_TIMESTAMP),
('Silvia Monti', 'Product Owner', CURRENT_TIMESTAMP),
('Dario Battaglia', 'Site Reliability Engineer', CURRENT_TIMESTAMP),
('Beatrice Farina', 'Quality Assurance Lead', CURRENT_TIMESTAMP),
('Massimo Rizzi', 'Enterprise Architect', CURRENT_TIMESTAMP),
('Valeria Caruso', 'Technical Writer', CURRENT_TIMESTAMP),
('Nicola De Luca', 'Release Manager', CURRENT_TIMESTAMP),
('Elisa Martini', 'Software Test Engineer', CURRENT_TIMESTAMP),
('Simone Gatti', 'Cloud Security Engineer', CURRENT_TIMESTAMP),
('Alessia Bernardi', 'IT Compliance Specialist', CURRENT_TIMESTAMP),

-- German Names
('Hans Mueller', 'Principal Software Engineer', CURRENT_TIMESTAMP),
('Wolfgang Schmidt', 'Security Operations Lead', CURRENT_TIMESTAMP),
('Klaus Weber', 'Technical Architect', CURRENT_TIMESTAMP),
('Gerhard Fischer', 'DevOps Team Lead', CURRENT_TIMESTAMP),
('Dieter Wagner', 'Systems Security Engineer', CURRENT_TIMESTAMP),
('Markus Becker', 'Cloud Platform Engineer', CURRENT_TIMESTAMP),
('Stefan Hoffmann', 'Software Development Manager', CURRENT_TIMESTAMP),
('Thomas Schulz', 'Integration Specialist', CURRENT_TIMESTAMP),
('Michael Koch', 'Database Security Specialist', CURRENT_TIMESTAMP),
('Andreas Bauer', 'Infrastructure Security Engineer', CURRENT_TIMESTAMP),
('Jürgen Richter', 'Senior Systems Engineer', CURRENT_TIMESTAMP),
('Werner Klein', 'Application Security Engineer', CURRENT_TIMESTAMP),
('Rainer Wolf', 'Network Engineer', CURRENT_TIMESTAMP),
('Erich Schröder', 'IT Security Architect', CURRENT_TIMESTAMP),
('Karl Neumann', 'Software Quality Engineer', CURRENT_TIMESTAMP),
('Sabine Meyer', 'Data Protection Specialist', CURRENT_TIMESTAMP),
('Monika Krause', 'Agile Project Manager', CURRENT_TIMESTAMP),
('Ursula Schwarz', 'Information Systems Security Officer', CURRENT_TIMESTAMP),
('Helga Zimmermann', 'Requirements Engineer', CURRENT_TIMESTAMP),
('Ingrid Schmitt', 'Configuration Manager', CURRENT_TIMESTAMP),
('Petra Lange', 'IT Governance Specialist', CURRENT_TIMESTAMP),
('Renate Krüger', 'Quality Management Lead', CURRENT_TIMESTAMP),
('Brigitte Hartmann', 'Documentation Specialist', CURRENT_TIMESTAMP),
('Erika Werner', 'Process Automation Engineer', CURRENT_TIMESTAMP),
('Heinrich Schmitz', 'Security Compliance Officer', CURRENT_TIMESTAMP),
('Otto Meier', 'Infrastructure Manager', CURRENT_TIMESTAMP),
('Fritz Lehmann', 'Systems Integration Engineer', CURRENT_TIMESTAMP),
('Walter König', 'Technical Operations Specialist', CURRENT_TIMESTAMP),
('Gustav Huber', 'Enterprise Solutions Architect', CURRENT_TIMESTAMP),
('Wilhelm Braun', 'Cloud Operations Engineer', CURRENT_TIMESTAMP),
('Manfred Berg', 'IT Risk Analyst', CURRENT_TIMESTAMP),
('Rudolf Fuchs', 'Cybersecurity Engineer', CURRENT_TIMESTAMP),
('Ernst Keller', 'Platform Engineer', CURRENT_TIMESTAMP),
('Hermann Vogel', 'Security Systems Architect', CURRENT_TIMESTAMP),
('Kurt Frank', 'Technical Support Lead', CURRENT_TIMESTAMP),
('Günther Berger', 'Systems Administrator', CURRENT_TIMESTAMP),
('Ludwig Kaiser', 'Network Operations Engineer', CURRENT_TIMESTAMP),
('Helmut Schuster', 'IT Auditor', CURRENT_TIMESTAMP); 