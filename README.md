# FilmHub - Complete Movie Ticketing and Management System
## Project Overview
FilmHub is a comprehensive, full-stack movie ticketing and cinema management platform that simulates a complete commercial movie industry ecosystem. Built with a classic Windows 98-inspired interface, this system supports multiple user roles and provides end-to-end functionality for the entire movie lifecycle.

## Core Features
### 🎬 Multi-Role System
- General Users : Browse movies, search films, purchase tickets, manage orders, recharge accounts
- Cinema Managers : Register cinemas, manage auditoriums, schedule screenings, track income statistics, handle check-ins
- Film Publishers : Release new movies, manage film catalogs, track box office performance
- System Administrators : Platform-level management and oversight
### 🏪 Cinema Management
- Cinema Registration : Complete cinema setup with location-based addressing system
- Auditorium Management : Configure seating layouts, visual effects capabilities
- Screening Scheduling : Plan movie showtimes with pricing and capacity management
- Revenue Tracking : Daily and monthly income analytics for business insights
### 🎥 Film Publishing
- Movie Release : Comprehensive film information including types, languages, visual effects
- Box Office Tracking : Real-time revenue monitoring for publishers
- Release Management : Control film availability windows (upcoming, screening, finished)
- Multi-language & Format Support : Diverse viewing options for audiences
### 🎟️ Ticketing System
- Real-time Seat Selection : Interactive seat maps with availability status
- Multi-criteria Search : Filter by location, time, language, format
- Order Management : Complete purchase flow with payment integration
- Order Status Tracking : Real-time updates on ticket status
## Technical Architecture
### 🛠️ Technology Stack
- Frontend : HTML5, CSS3, Vanilla JavaScript (no frameworks)
- Backend : Java Servlets with embedded Tomcat server
- Database : HSQLDB with comprehensive relational design
- Deployment : Complete startup/shutdown scripts for easy setup
### 📊 Database Design
- 18 Core Tables : Complex relational model with proper constraints
- Multi-level Relationships : Province → City → Cinema → Auditorium → Schedule hierarchy
- Advanced Views : 15+ database views for complex business logic
- Data Integrity : Comprehensive foreign key relationships and constraints
### 🔐 Security & Validation
- Role-based Authentication : Separate login systems for each user type
- Input Validation : Client and server-side data validation
- Session Management : Secure user session handling
- Error Handling : Comprehensive exception management
## Business Logic Highlights
### 🌍 Geographic System
- Hierarchical location management (Province → City → Cinema)
- Regional cinema distribution and management
- Location-based film availability
### 💰 Financial System
- User wallet management with recharge functionality
- Cinema revenue tracking and analytics
- Publisher box office calculations
- Multi-tier pricing strategies
### 🎯 Advanced Features
- Real-time Seat Availability : Dynamic seat selection with conflict prevention
- Multi-format Support : Various language and visual effect combinations
- Status-based Filtering : Browse films by release status (upcoming, screening, finished)
- Order Lifecycle : Complete order management with status tracking
## Project Value
This project demonstrates advanced full-stack development capabilities including:

- Complex database design and optimization
- Multi-tier application architecture
- Real-world business logic implementation
- User experience design and optimization
- Production-ready deployment configuration
FilmHub represents a complete commercial-grade application suitable for educational demonstration, technical assessment, or as a foundation for real-world movie ticketing platforms.
