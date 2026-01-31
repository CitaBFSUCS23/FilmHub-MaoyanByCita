import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;
import jakarta.servlet.http.Part;
import java.io.InputStream;
import java.nio.file.Files;
import java.io.File;

@WebServlet("/MyRelease")
@MultipartConfig(
    maxFileSize = 1024 * 1024 * 10,      // 10MB 最大文件大小
    maxRequestSize = 1024 * 1024 * 50,   // 50MB 最大请求大小
    fileSizeThreshold = 1024 * 1024      // 1MB 文件大小阈值，超过此大小将写入磁盘
)
public class MyReleaseServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        // 从请求头获取发行商ID
        String publisherId = request.getHeader("X-Publisher-Id");
        if (publisherId == null) {
            sendError(response, "发行商请先登录");
            return;
        }
        String searchKeyword = request.getParameter("search");
        String pageParam = request.getParameter("page");
        
        try (PrintWriter out = response.getWriter()) {
            JSONObject result = new JSONObject();
            
            // 获取影片列表数据
            JSONObject filmData = getFilmList(publisherId, searchKeyword, pageParam);
            
            // 获取影片类型、语言、视觉效果数据
            JSONObject metadata = getMetadata();
            
            // 获取新影片ID
            JSONObject newFilmInfo = getNewFilmInfo(publisherId);
            
            result.put("status", "success");
            result.put("publisherId", publisherId);
            result.put("filmList", filmData.getJSONArray("films"));
            result.put("totalPages", filmData.getInt("totalPages"));
            result.put("currentPage", filmData.getInt("currentPage"));
            result.put("totalFilms", filmData.getInt("totalFilms"));
            result.put("filmTypes", metadata.getJSONArray("filmTypes"));
            result.put("filmLanguages", metadata.getJSONArray("filmLanguages"));
            result.put("visualEffects", metadata.getJSONArray("visualEffects"));
            result.put("newFilmId", newFilmInfo.getInt("newFilmId"));
            result.put("displayNewFilmId", newFilmInfo.getString("displayNewFilmId"));
            
            out.print(result.toString());
        } catch (Exception e) {
            sendError(response, "服务器错误: " + e.getMessage());
        }
    }
    
    private JSONObject getFilmList(String publisherId, String searchKeyword, String pageParam) throws SQLException {
        int pageSize = 5;
        int currentPage = 1;
        
        if (pageParam != null && !pageParam.trim().isEmpty()) {
            try {
                currentPage = Integer.parseInt(pageParam);
                if (currentPage < 1) currentPage = 1;
            } catch (NumberFormatException e) {
                currentPage = 1;
            }
        }
        
        int offset = (currentPage - 1) * pageSize;
        int totalFilms = 0;
        
        JSONObject result = new JSONObject();
        JSONArray films = new JSONArray();
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            // 获取总记录数
            String countSql = "SELECT COUNT(*) FROM view_Film_All WHERE Film_Publisher_ID = ?";
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                countSql += " AND Film_Name LIKE ?";
            }
            pstmt = conn.prepareStatement(countSql);
            pstmt.setString(1, publisherId);
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                pstmt.setString(2, "%" + searchKeyword + "%");
            }
            rs = pstmt.executeQuery();
            if (rs.next()) totalFilms = rs.getInt(1);
            DatabaseUtil.closeResources(rs, pstmt, null);
            
            // 查询当前页的影片数据
            String filmSql = "SELECT Film_ID, Film_Name, Film_Publish_Date, Film_Release_Date, Film_Finished_Date, Film_Duration_Min, Film_Intro, Film_Min_Fare, Film_Box_Office, Film_Type_Names, Film_Language_Names, Visual_Effect_Names FROM view_Film_All WHERE Film_Publisher_ID = ?";
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                filmSql += " AND Film_Name LIKE ?";
            }
            filmSql += " ORDER BY Film_ID DESC LIMIT ? OFFSET ?";
            
            pstmt = conn.prepareStatement(filmSql);
            pstmt.setString(1, publisherId);
            int paramIndex = 2;
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                pstmt.setString(paramIndex++, "%" + searchKeyword + "%");
            }
            pstmt.setInt(paramIndex++, pageSize);
            pstmt.setInt(paramIndex++, offset);
            rs = pstmt.executeQuery();
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            
            while (rs.next()) {
                JSONObject film = new JSONObject();
                film.put("publisherId", publisherId);
                // 确保返回的filmId是6位数字格式
                String filmId = rs.getString("Film_ID");
                String formattedFilmId = String.format("%06d", Integer.parseInt(filmId));
                film.put("filmId", formattedFilmId);
                film.put("filmName", rs.getString("Film_Name"));
                film.put("publishDate", sdf.format(rs.getDate("Film_Publish_Date")));
                film.put("releaseDate", sdf.format(rs.getDate("Film_Release_Date")));
                film.put("finishedDate", sdf.format(rs.getDate("Film_Finished_Date")));
                film.put("duration", rs.getInt("Film_Duration_Min"));
                film.put("synopsis", rs.getString("Film_Intro"));
                film.put("minFare", rs.getDouble("Film_Min_Fare"));
                film.put("boxOffice", rs.getDouble("Film_Box_Office"));
                film.put("filmTypes", rs.getString("Film_Type_Names"));
                film.put("filmLanguages", rs.getString("Film_Language_Names"));
                film.put("visualEffects", rs.getString("Visual_Effect_Names"));
                films.put(film);
            }
            
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
        
        int totalPages = (int) Math.ceil((double) totalFilms / pageSize);
        
        result.put("films", films);
        result.put("totalPages", totalPages);
        result.put("currentPage", currentPage);
        result.put("totalFilms", totalFilms);
        
        return result;
    }
    
    private JSONObject getMetadata() throws SQLException {
        JSONObject metadata = new JSONObject();
        JSONArray filmTypes = new JSONArray();
        JSONArray filmLanguages = new JSONArray();
        JSONArray visualEffects = new JSONArray();
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            stmt = conn.createStatement();
            
            // 查询影片类型
            rs = stmt.executeQuery("SELECT Film_Type_Name FROM table_Film_Type");
            while (rs.next()) {
                filmTypes.put(rs.getString("Film_Type_Name"));
            }
            DatabaseUtil.closeResources(rs, null, null);
            
            // 查询影片语言
            rs = stmt.executeQuery("SELECT Film_Language_Name FROM table_Film_Language");
            while (rs.next()) {
                filmLanguages.put(rs.getString("Film_Language_Name"));
            }
            DatabaseUtil.closeResources(rs, null, null);
            
            // 查询视觉效果
            rs = stmt.executeQuery("SELECT Visual_Effect_Name FROM table_Visual_Effect");
            while (rs.next()) {
                visualEffects.put(rs.getString("Visual_Effect_Name"));
            }
            
        } finally {
            DatabaseUtil.closeResources(rs, stmt, conn);
        }
        
        metadata.put("filmTypes", filmTypes);
        metadata.put("filmLanguages", filmLanguages);
        metadata.put("visualEffects", visualEffects);
        
        return metadata;
    }
    
    private JSONObject getNewFilmInfo(String publisherId) throws SQLException {
        JSONObject result = new JSONObject();
        int publisherIdInt = Integer.parseInt(publisherId);
        int newFilmID = 0;
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            
            String sql = "SELECT MAX(Film_ID) FROM table_Film WHERE Film_Publisher_ID = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, publisherIdInt);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                Integer maxFilmId = rs.getInt(1);
                newFilmID = rs.wasNull() ? 1 : maxFilmId + 1;
            }
            
        } finally {
            DatabaseUtil.closeResources(rs, pstmt, conn);
        }
        
        String displayNewFilmId = String.format("%06d%06d", publisherIdInt, newFilmID);
        
        result.put("newFilmId", newFilmID);
        result.put("displayNewFilmId", displayNewFilmId);
        
        return result;
    }
    
    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        JSONObject error = new JSONObject();
        error.put("status", "error");
        error.put("message", message);
        response.getWriter().print(error.toString());
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("UTF-8");

        try (PrintWriter out = response.getWriter()) {
            JSONObject result = new JSONObject();
            
            // 检查是否有文件上传
            Part posterPart = request.getPart("filmPoster");
            if (posterPart == null || posterPart.getSize() == 0) {
                sendError(response, "请上传影片海报");
                return;
            }
            
            // 解析表单数据
            Map<String, Object> formData = parseFormData(request);
            if (formData == null) {
                sendError(response, "表单解析失败");
                return;
            }
            
            // 验证表单数据
            String validationError = validateFormData(formData);
            if (validationError != null) {
                sendError(response, validationError);
                return;
            }
            
            // 保存影片数据到数据库
            boolean success = saveFilmToDatabase(formData);
            if (success) {
                result.put("status", "success");
                result.put("message", "影片发布成功！");
            } else {
                sendError(response, "影片发布失败，请重试");
                return;
            }
            
            out.print(result.toString());
        } catch (Exception e) {
            sendError(response, "服务器错误: " + e.getMessage());
        }
    }
    
    private Map<String, Object> parseFormData(HttpServletRequest request) {
        Map<String, Object> formData = new HashMap<>();
        
        try {
            // 处理普通表单字段
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String fieldName = parameterNames.nextElement();
                String[] fieldValues = request.getParameterValues(fieldName);
                
                // 处理多选框
                if (fieldName.equals("filmType") || fieldName.equals("filmLanguage") || fieldName.equals("visualEffect")) {
                    List<String> values = (List<String>) formData.get(fieldName);
                    if (values == null) {
                        values = new ArrayList<>();
                        formData.put(fieldName, values);
                    }
                    for (String value : fieldValues) {
                        values.add(value);
                    }
                } else {
                    // 单值字段
                    formData.put(fieldName, fieldValues[0]);
                }
            }
            
            // 处理文件上传
            Part posterPart = request.getPart("filmPoster");
            if (posterPart != null && posterPart.getSize() > 0) {
                formData.put("filmPoster", posterPart);
            }
            
            return formData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private String validateFormData(Map<String, Object> formData) {
        // 验证影片类型（1-3种）
        List<String> filmTypes = (List<String>) formData.get("filmType");
        if (filmTypes == null || filmTypes.size() < 1 || filmTypes.size() > 3) {
            return "请选择1-3种影片类型";
        }

        List<String> filmLanguages = (List<String>) formData.get("filmLanguage");
        if (filmLanguages == null || filmLanguages.isEmpty()) {
            return "请至少选择一种支持语言";
        }

        List<String> filmVisualEffects = (List<String>) formData.get("visualEffect");
        if (filmVisualEffects == null || filmVisualEffects.isEmpty()) {
            return "请至少选择一种视觉效果";
        }
        
        // 验证日期
        String releaseDate = (String) formData.get("releaseDate");
        String endDate = (String) formData.get("endDate");
        if (endDate.compareTo(releaseDate) < 0) {
            return "结映日期不能早于首映日期";
        }
        
        // 验证票价格式
        String minFare = (String) formData.get("minFare");
        if (!minFare.matches("^\\d{1,2}(\\.\\d{1,2})?$")) {
            return "票价格式不正确，应为不超过100的两位小数";
        }
        
        return null;
    }
    
    private boolean saveFilmToDatabase(Map<String, Object> formData) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);
            
            String publisherId = (String) formData.get("publisherId");
            String filmId = (String) formData.get("filmId");
            String filmName = (String) formData.get("filmName");
            String filmDuration = (String) formData.get("filmDuration");
            String releaseDate = (String) formData.get("releaseDate");
            String endDate = (String) formData.get("endDate");
            String filmDescription = (String) formData.get("filmDescription");
            String minFare = (String) formData.get("minFare");
            
            List<String> filmTypes = (List<String>) formData.get("filmType");
            List<String> filmLanguages = (List<String>) formData.get("filmLanguage");
            List<String> visualEffects = (List<String>) formData.get("visualEffect");
            
            // 插入影片基本信息
            String filmSql = "INSERT INTO table_Film (Film_ID, Film_Publisher_ID, Film_Name, Film_Duration_Min,Film_Release_Date, Film_Finished_Date, Film_Intro, Film_Min_Fare, Film_Publish_Date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)";
            pstmt = conn.prepareStatement(filmSql);
            pstmt.setInt(1, Integer.parseInt(filmId));
            pstmt.setInt(2, Integer.parseInt(publisherId));
            pstmt.setString(3, filmName);
            pstmt.setInt(4, Integer.parseInt(filmDuration));
            pstmt.setDate(5, java.sql.Date.valueOf(releaseDate));
            pstmt.setDate(6, java.sql.Date.valueOf(endDate));
            pstmt.setString(7, filmDescription);
            pstmt.setDouble(8, Double.parseDouble(minFare));
            pstmt.executeUpdate();
            DatabaseUtil.closeResources(null, pstmt, null);
            
            // 插入影片类型关联
            for (String filmType : filmTypes) {
                String typeSql = "INSERT INTO table_Film_Types_Association (Film_Publisher_ID, Film_ID, Film_Type_Name) VALUES (?, ?, ?)";
                pstmt = conn.prepareStatement(typeSql);
                pstmt.setInt(1, Integer.parseInt(publisherId));
                pstmt.setInt(2, Integer.parseInt(filmId));
                pstmt.setString(3, filmType);
                pstmt.executeUpdate();
                DatabaseUtil.closeResources(null, pstmt, null);
            }
            
            // 插入影片语言关联
            for (String filmLanguage : filmLanguages) {
                String languageSql = "INSERT INTO table_Film_Languages_Association (Film_Publisher_ID, Film_ID, Film_Language_Name) VALUES (?, ?, ?)";
                pstmt = conn.prepareStatement(languageSql);
                pstmt.setInt(1, Integer.parseInt(publisherId));
                pstmt.setInt(2, Integer.parseInt(filmId));
                pstmt.setString(3, filmLanguage);
                pstmt.executeUpdate();
                DatabaseUtil.closeResources(null, pstmt, null);
            }
            
            // 插入视觉效果关联
            for (String visualEffect : visualEffects) {
                String effectSql = "INSERT INTO table_Film_Visual_Effects_Association (Film_Publisher_ID, Film_ID, Visual_Effect_Name) VALUES (?, ?, ?)";
                pstmt = conn.prepareStatement(effectSql);
                pstmt.setInt(1, Integer.parseInt(publisherId));
                pstmt.setInt(2, Integer.parseInt(filmId));
                pstmt.setString(3, visualEffect);
                pstmt.executeUpdate();
                DatabaseUtil.closeResources(null, pstmt, null);
            }
            
            // 保存海报文件
            Part posterPart = (Part) formData.get("filmPoster");
            if (posterPart != null && posterPart.getSize() > 0) {
                savePosterFile(posterPart, publisherId, filmId);
            }
            
            conn.commit();
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            DatabaseUtil.closeResources(null, pstmt, conn);
        }
    }
    
    private void savePosterFile(Part posterPart, String publisherId, String filmId) {
        try {
            // 创建上传目录
            String uploadDir = new File("uploads/films/" + publisherId + "/").getAbsolutePath();
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 保存文件，确保影片ID补足6位前导0
            String formattedFilmId = String.format("%06d", Integer.parseInt(filmId));
            String fileName = formattedFilmId + ".jpg";
            File uploadedFile = new File(dir, fileName);
            
            // 使用Files.copy来保存文件
            try (InputStream input = posterPart.getInputStream()) {
                Files.copy(input, uploadedFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}