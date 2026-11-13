Logistic – Courier Tracking System

A web-based Courier Tracking System built using Java, Spring Boot, Spring Data JPA, and MySQL. The application allows users to register, log in, create courier bookings, track parcels, and manage their shipments. Administrators can manage parcel statuses through a dedicated admin dashboard.

🚀 Features

User Features

User registration and login.

Secure user authentication.

Create new courier/parcel bookings.

View parcel details and shipment status.

Track parcels using unique tracking IDs.

Search and filter parcel records.

Cancel parcels.

Admin Features

Dedicated admin login.

Separate admin dashboard.

View and manage parcel records.

Update parcel delivery statuses.

Manage courier tracking operations.

🛠️ Tech Stack

Technology

Purpose

Java 21

Backend programming

Spring Boot

Application framework

Spring Data JPA

Database interaction

Hibernate

ORM

MySQL 8

Relational database

Maven

Dependency management

IntelliJ IDEA

Development environment

HTML, CSS, JavaScript

Frontend (if applicable)

🏗️ Project Structure

Logistic/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/Logistic/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── entity/
│   │   │       ├── config/
│   │   │       └── LogisticApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── pom.xml
├── .gitignore
└── README.md

Note: Adjust the package and folder names above to match your actual project structure.

🗄️ Database Configuration

The project uses MySQL as its relational database.

Create the database:

CREATE DATABASE courier_db;

Configure the following properties in application.properties:

spring.application.name=Logistic

spring.datasource.url=jdbc:mysql://localhost:3306/courier_db
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

app.admin.email=${ADMIN_EMAIL:}
app.admin.password=${ADMIN_PASSWORD:}

Hibernate automatically manages the database schema based on the entity classes.

⚙️ Installation and Setup

Prerequisites

Make sure you have installed:

Java Development Kit (JDK 21)

MySQL 8.0 or later

IntelliJ IDEA

Git

Step 1: Clone the Repository

git clone https://github.com/YOUR_USERNAME/Courier-Tracking-System.git

Step 2: Open the Project

Open the cloned project in IntelliJ IDEA and allow Maven to download the required dependencies.

Step 3: Configure Environment Variables

Set the following environment variables before running the application:

Variable

Description

DB_PASSWORD

MySQL root password

ADMIN_EMAIL

Admin account email

ADMIN_PASSWORD

Admin account password

Never commit actual passwords or credentials to GitHub.

Step 4: Run the Application

Run the main Spring Boot class:

LogisticApplication.java

Or use Maven:

mvn spring-boot:run

The application will start on:

http://localhost:8080

🔄 Application Workflow

A new user registers for an account.

The user logs in to access their dashboard.

The user creates a courier booking.

The system generates a unique tracking ID.

The user tracks the parcel using the tracking ID.

The administrator logs in through the admin dashboard.

The administrator updates the parcel delivery status.

The user can view the updated status and cancel eligible parcels.

🔐 Security

Database and admin credentials are managed through environment variables.

User and admin access are separated by roles.

Sensitive configuration values should not be committed to version control.

🎯 Project Objectives

Understand real-world backend application development.

Implement CRUD operations using Spring Boot and JPA.

Practice relational database management with MySQL.

Implement user and admin workflows.

Understand REST API development and backend architecture.