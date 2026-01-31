import java.sql.*;

public class DatabaseUtil {
    // HSQLDB连接配置（服务器模式）
    public static final String JDBC_URL = "jdbc:hsqldb:hsql://localhost:9011/FilmHub;sql.enforce_strict_size=true;charset=utf-8;hsqldb.default_table_type=cached";
    public static final String USER_NAME = "SA";
    public static final String PASSWORD = "";
    
    /**
     * 获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        loadJDBCDriver();
        return DriverManager.getConnection(JDBC_URL, USER_NAME, PASSWORD);
    }
    
    /**
     * 关闭数据库资源
     */
    public static void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        try { if (rs != null) rs.close(); } catch (SQLException e) {}
        try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
        try { if (conn != null) conn.close(); } catch (SQLException e) {}
    }
    
    /**
     * 加载数据库驱动
     */
    public static boolean loadJDBCDriver() {
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}