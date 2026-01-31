import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TomcatStarter {

    public static void main(String[] args) throws LifecycleException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 1. 创建 Tomcat 实例
        Tomcat tomcat = new Tomcat();

        // 2. 设置端口（9001）
        tomcat.setPort(9001);
        // 关闭默认连接器的 AJP 协议（避免端口冲突）
        tomcat.getConnector();

        // 3. 设置 Web 应用的基础目录
        String webappDir = new File("src").getAbsolutePath();
        
        // 4. 创建 Web 应用上下文（"/" 表示根路径，访问 http://localhost:9001 即可）
        Context context = tomcat.addWebapp("/", webappDir);
        
        // 5. 配置静态资源访问路径
        // 将/uploads路径映射到项目根目录下的uploads文件夹
        StandardRoot resources = new StandardRoot(context);
        // 添加uploads目录映射
        String uploadsDir = new File("uploads").getAbsolutePath();
        File uploadsFolder = new File(uploadsDir);
        if (!uploadsFolder.exists()) {
            uploadsFolder.mkdirs();
        }
        resources.addPreResources(new DirResourceSet(resources, "/uploads", uploadsDir, "/"));
        context.setResources(resources);
        
        // 6. 所有Servlet已通过@WebServlet注解注册，无需手动映射

        // 7. 启动 Tomcat 服务器
        tomcat.start();
        System.out.println("Tomcat 11 嵌入式服务器已启动，访问地址：http://localhost:9001");

        // 8. 启动定时任务，每分钟执行一次维护存储过程（自动取消过期订单和更新完成订单）
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                executeMaintenanceProcedures();
                // 格式化当前时间并打印成功消息
                String currentTime = LocalDateTime.now().format(formatter);
                System.out.println(currentTime + " - 维护存储过程执行成功");
            } catch (SQLException e) {
                // 格式化当前时间并打印错误消息
                String currentTime = LocalDateTime.now().format(formatter);
                System.err.println(currentTime + " - 执行维护存储过程失败: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);
        
        // 9. 让服务器保持运行（阻塞主线程）
        tomcat.getServer().await();
        
        // 10. 关闭服务器时，关闭定时任务
        scheduler.shutdown();
    }
    
    /**
     * 执行维护存储过程（自动取消过期订单和更新完成订单）
     */
    private static void executeMaintenanceProcedures() throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            // 获取数据库连接
            conn = DatabaseUtil.getConnection();
            
            // 执行自动取消过期订单的存储过程
            String cancelSql = "CALL procedure_Shift_Unpaid_Orders()";
            pstmt = conn.prepareStatement(cancelSql);
            pstmt.executeUpdate();
            
            // 执行自动更新完成订单的存储过程
            String completeSql = "CALL procedure_Shift_Completed_Orders()";
            pstmt = conn.prepareStatement(completeSql);
            pstmt.executeUpdate();
        } finally {
            // 使用DatabaseUtil的方法关闭资源
            DatabaseUtil.closeResources(null, pstmt, conn);
        }
    }
}