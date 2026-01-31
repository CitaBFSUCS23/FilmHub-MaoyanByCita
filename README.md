# FilmHub - Complete Movie Ticketing and Management System

<div align="center">

![FilmHub Logo](https://img.shields.io/badge/FilmHub-Movie%20Ticketing%20System-blue)
![Java](https://img.shields.io/badge/Java-Servlet-orange)
![HSQLDB](https://img.shields.io/badge/Database-HSQLDB-green)
![Windows 98 Style](https://img.shields.io/badge/Style-Windows%2098-nostalgia)

**A comprehensive, full-stack movie ticketing platform with multi-role support and complete business logic**

</div>

## 📋 Table of Contents
- [Project Overview](#-project-overview)
- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Quick Start](#-quick-start)
- [Installation Guide](#-installation-guide)
- [User Roles](#-user-roles)
- [Database Schema](#-database-schema)
- [Project Value](#-project-value)
- [Development Notes](#-development-notes)
- [Troubleshooting](#-troubleshooting)

## 🎬 Project Overview

FilmHub is a sophisticated movie ticketing and cinema management system that simulates a complete commercial movie industry ecosystem. Built with a classic Windows 98-inspired interface, this platform supports multiple user roles and provides end-to-end functionality for the entire movie lifecycle - from film publishing to ticket purchasing and cinema management.

### Key Highlights
- **Multi-role System**: General users, cinema managers, film publishers
- **Complete Business Logic**: Full transaction flow with real-time seat management
- **Advanced Database Design**: 18 relational tables with complex constraints
- **Production-Ready Architecture**: Embedded Tomcat server with HSQLDB

## ✨ Features

### 🎟️ **Ticketing System**
- Real-time seat selection with availability tracking
- Multi-criteria search (location, time, language, format)
- Complete purchase flow with payment integration
- Order status tracking and management

### 🏪 **Cinema Management**
- Cinema registration with location-based addressing
- Auditorium configuration and seating layout management
- Movie scheduling with pricing strategies
- Revenue analytics (daily/monthly income tracking)

### 🎥 **Film Publishing**
- Comprehensive movie release management
- Box office performance monitoring
- Multi-language and format support
- Release window control (upcoming, screening, finished)

### 👥 **User Management**
- Role-based authentication system
- Account wallet with recharge functionality
- Personal information management
- Order history and status tracking

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Frontend** | HTML5, CSS3, Vanilla JavaScript | - |
| **Backend** | Java Servlets | JDK 8+ |
| **Database** | HSQLDB | 2.x |
| **Web Server** | Embedded Tomcat | - |
| **Build Tool** | Manual compilation | - |

## 🚀 Quick Start

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- Windows Operating System
- 2GB RAM minimum (4GB+ recommended)

### Installation Steps (Windows)

1. **Clone the repository**
   ```bash
   git clone https://github.com/CitaBFSUCS23/FilmHub-MaoyanByCita.git
   cd FilmHub-MaoyanByCita
   ```

2. **Compile the project**
   ```cmd
   runServer\compile_all.bat
   ```

3. **Start database server** (keep window open)
   ```cmd
   runServer\start_hsqldb_server.bat
   ```

4. **Initialize database** (new terminal)
   ```cmd
   runServer\runManagerSwing.bat
   ```
   - Execute SQL scripts in order: Tables → Views → Procedures → Triggers → Data

5. **Start web application**
   ```cmd
   runServer\start_Tomcat_server.bat
   ```

6. **Access application**
   - Open browser: `http://localhost:9001`

## 👥 User Roles

### General Users
- **Features**: Movie browsing, ticket purchasing, account management
- **Access**: Public movie catalog, personal order history

### Cinema Managers
- **Features**: Cinema registration, auditorium management, scheduling
- **Access**: Cinema-specific management tools, revenue analytics

### Film Publishers
- **Features**: Movie release, box office tracking, catalog management
- **Test Account**: ID `119004`, Password `119004`

## 🗃️ Database Schema

### Core Tables (18 Total)
- `table_Account` - User accounts and wallet management
- `table_Film` - Movie information and release details
- `table_Cinema` - Cinema locations and information
- `table_Schedule` - Movie screening schedules
- `table_Order` - Ticket orders and status tracking
- `table_Manage` - Cinema management permissions
- `table_Publisher` - Film publisher accounts

### Advanced Features
- **15+ Database Views** for complex business logic
- **Proper Constraints** with foreign key relationships
- **Hierarchical Structure** (Province → City → Cinema → Auditorium)

## 💰 Project Value

### Technical Assessment
This project demonstrates advanced full-stack development capabilities:
- Complex database design and optimization
- Multi-tier application architecture
- Real-world business logic implementation
- User experience design and optimization

## 📖 Installation Guide (Detailed)

### System Requirements
- **JDK 8 or higher**
- **Windows OS** (tested on Windows 10/11)
- **Ports 9001 & 9011** available
- **500MB disk space**

### Step-by-Step Deployment

1. **Environment Verification**
   ```cmd
   java -version
   javac -version
   ```

2. **Database Initialization Sequence**
   ```sql
   -- Execute in Database Manager in this order:
   SOURCE 'db/hsqldb_schema_TABLES.sql'
   SOURCE 'db/hsqldb_schema_VIEWS.sql'
   SOURCE 'db/hsqldb_schema_PROCEDURE.sql'
   SOURCE 'db/hsqldb_schema_TRIGGERS.sql'
   SOURCE 'db/hsqldb_DATA.sql'
   ```

3. **Service Startup Order**
   1. HSQLDB Server (`start_hsqldb_server.bat`)
   2. Database Initialization (via Database Manager)
   3. Tomcat Server (`start_Tomcat_server.bat`)

## 🔧 Development Notes

### Project Structure
```
FilmHub/
├── src/                    # Source code
│   ├── WEB-INF/           # Web configuration & Java classes
│   ├── General/           # User-facing features
│   ├── Manager/           # Cinema management
│   ├── Publisher/         # Film publishing
│   └── Login/             # Authentication system
├── db/                    # Database scripts
│   ├── hsqldb_schema_TABLES.sql
│   ├── hsqldb_schema_VIEWS.sql
│   ├── hsqldb_schema_PROCEDURE.sql
│   ├── hsqldb_schema_TRIGGERS.sql
│   └── hsqldb_DATA.sql
├── lib/                   # Dependencies (JAR files)
└── runServer/            # Deployment scripts
```

### Key Design Patterns
- **MVC Architecture**: Separation of concerns
- **Role-Based Access Control**: Multi-level permission system
- **Real-time Updates**: Dynamic seat availability
- **Error Handling**: Comprehensive exception management

## 🐛 Troubleshooting

### Common Issues

#### Port Conflicts
```cmd
# Check port usage
netstat -ano | findstr :9001
netstat -ano | findstr :9011

# Kill conflicting processes
taskkill /PID <process_id> /F
```

#### Database Connection Errors
- Verify HSQLDB server is running on port 9011
- Check database URL: `jdbc:hsqldb:hsql://localhost:9011/FilmHub`
- Ensure all SQL scripts executed successfully

#### Compilation Issues
- Verify JDK installation and JAVA_HOME variable
- Check file permissions in project directory
- Ensure all JAR files present in `lib/` directory

### Performance Optimization
```batch
# Increase JVM memory for large-scale testing
java -Xmx1024m -Xms512m -cp "%CLASSPATH%" TomcatStarter
```

## 📞 Support

For technical support or questions about this project:
- **Developer**: Cita_Stargazer
- **Bilibili**: [Cita_Stargazer](https://space.bilibili.com/393808049)
- **Project Type**: University Graduation Project / Commercial Demo

---

<div align="center">

**FilmHub** - A comprehensive demonstration of full-stack development capabilities with real-world business logic implementation.

*Built with ❤️ using Java Servlets, HSQLDB, and classic web technologies* 

</div>
