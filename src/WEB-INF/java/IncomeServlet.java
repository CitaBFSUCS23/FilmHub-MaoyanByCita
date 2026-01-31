import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/IncomeServlet")
public class IncomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        
        try {
            // 从请求头获取影院信息和年月
            String cinemaProvinceCodeStr = request.getHeader("X-Cinema-Province-Code");
            String cinemaCityCodeStr = request.getHeader("X-Cinema-City-Code");
            String cinemaIdStr = request.getHeader("X-Cinema-ID");
            String yearStr = request.getHeader("X-Year");
            String monthStr = request.getHeader("X-Month");
            
            // 转换参数类型
            int cinemaProvinceCode = Integer.parseInt(cinemaProvinceCodeStr);
            int cinemaCityCode = Integer.parseInt(cinemaCityCodeStr);
            int cinemaId = Integer.parseInt(cinemaIdStr);
            int year = Integer.parseInt(yearStr);
            int month = Integer.parseInt(monthStr);
            
            // 验证年份和月份范围
            if (year < 2000 || year > 2100 || month < 1 || month > 12) {
                result.put("status", "error");
                result.put("message", "年份或月份超出有效范围");
                response.getWriter().print(result.toString());
                return;
            }
            
            // 获取数据库连接
            Connection conn = DatabaseUtil.getConnection();
            
            // 查询每日收入数据
            Map<String, Double> dailyIncome = getDailyIncome(conn, cinemaProvinceCode, cinemaCityCode, cinemaId, year, month);
            
            // 构建响应结果
            result.put("status", "success");
            result.put("dailyIncome", new JSONObject(dailyIncome));
            
            // 关闭数据库连接
            DatabaseUtil.closeResources(null, null, conn);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "error");
            result.put("message", "服务器内部错误：" + e.getMessage());
        }
        
        response.getWriter().print(result.toString());
    }
    
    /**
     * 查询每日收入数据
     */
    private Map<String, Double> getDailyIncome(Connection conn, int provinceCode, int cityCode, int cinemaId, int year, int month) {
        Map<String, Double> dailyIncome = new HashMap<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            // 查询每日收入数据
            String sql = "SELECT show_date, daily_income FROM view_Cinema_Daily_Income WHERE (Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, YEAR(show_date), MONTH(show_date)) = (?, ?, ?, ?, ?) ORDER BY show_date";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, provinceCode);
            pstmt.setInt(2, cityCode);
            pstmt.setInt(3, cinemaId);
            pstmt.setInt(4, year);
            pstmt.setInt(5, month);
            rs = pstmt.executeQuery();
            
            // 格式化日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            
            // 处理查询结果
            while (rs.next()) {
                java.sql.Date date = rs.getDate("show_date");
                double income = rs.getDouble("daily_income");
                String dateStr = sdf.format(date);
                dailyIncome.put(dateStr, income);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 清理资源
            DatabaseUtil.closeResources(rs, pstmt, null);
        }
        
        return dailyIncome;
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // POST请求返回错误信息
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject result = new JSONObject();
        result.put("status", "error");
        result.put("message", "请使用GET方法请求");
        
        response.getWriter().print(result.toString());
    }
}