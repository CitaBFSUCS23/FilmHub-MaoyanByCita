-- HSQLDB 视图

-- 发行商页面视图，用于AboutPublisher
CREATE VIEW IF NOT EXISTS view_About_Publisher AS
SELECT Publisher_ID, Publisher_Name, Publisher_Nationality FROM table_Publisher;

-- 影片所有信息视图，用于MyRelease，和BuyTicket
CREATE VIEW IF NOT EXISTS view_Film_All AS
SELECT 
    F.Film_Publisher_ID,
    F.Film_ID,
    F.Film_Name,
    F.Film_Publish_Date,
    F.Film_Release_Date,
    F.Film_Finished_Date,
    F.Film_Intro,
    F.Film_Duration_Min,
    F.Film_Min_Fare,
    F.Film_Box_Office,
    GROUP_CONCAT(DISTINCT FTA.Film_Type_Name) AS Film_Type_Names,
    GROUP_CONCAT(DISTINCT FLA.Film_Language_Name) AS Film_Language_Names,
    GROUP_CONCAT(DISTINCT FVEA.Visual_Effect_Name) AS Visual_Effect_Names
FROM 
    table_Film F
JOIN 
    table_Film_Types_Association FTA ON (F.Film_Publisher_ID, F.Film_ID) = (FTA.Film_Publisher_ID, FTA.Film_ID)
JOIN 
    table_Film_Languages_Association FLA ON (F.Film_Publisher_ID, F.Film_ID) = (FLA.Film_Publisher_ID, FLA.Film_ID)
JOIN 
    table_Film_Visual_Effects_Association FVEA ON (F.Film_Publisher_ID, F.Film_ID) = (FVEA.Film_Publisher_ID, FVEA.Film_ID)
GROUP BY 
    F.Film_Publisher_ID, F.Film_ID;

-- 影片所有信息视图，用于FilmScan
CREATE VIEW IF NOT EXISTS view_Film_Hot AS
SELECT 
    F.Film_Publisher_ID,
    F.Film_ID,
    F.Film_Name,
    F.Film_Intro,
    F.Film_Duration_Min,
    F.Film_Box_Office,
    GROUP_CONCAT(DISTINCT FTA.Film_Type_Name) AS Film_Type_Names,
    CASE 
        WHEN CURRENT_DATE < F.Film_Release_Date THEN 'UpComing'
        WHEN CURRENT_DATE BETWEEN F.Film_Release_Date AND F.Film_Finished_Date THEN 'HotFilming'
        ELSE 'HasFinished'
    END AS Release_Status
FROM 
    table_Film F
JOIN 
    table_Film_Types_Association FTA ON F.Film_Publisher_ID = FTA.Film_Publisher_ID AND F.Film_ID = FTA.Film_ID
GROUP BY 
        F.Film_Publisher_ID, F.Film_ID, F.Film_Name, F.Film_Intro, F.Film_Duration_Min, F.Film_Box_Office, 
        F.Film_Release_Date, F.Film_Finished_Date;

-- 排片视图，用于BuyTicket
CREATE VIEW IF NOT EXISTS view_Schedule_BuyTicket AS
SELECT 
    S.Schedule_Film_Publisher_ID,
    S.Schedule_Film_ID,
    S.Schedule_Cinema_Province_Code,
    S.Schedule_Cinema_City_Code,
    S.Schedule_Cinema_ID,
    C.Cinema_Name,
    S.Schedule_Auditorium_ID,
    S.Schedule_ID,
    S.Schedule_Film_Language,
    S.Schedule_Visual_Effect,
    S.Schedule_Show_Time,
    S.Schedule_Fare
FROM 
    table_Schedule S
JOIN
    table_Cinema C ON (S.Schedule_Cinema_Province_Code, S.Schedule_Cinema_City_Code, S.Schedule_Cinema_ID) = (C.Cinema_Province_Code, C.Cinema_City_Code, C.Cinema_ID)
WHERE
    S.Schedule_Show_Time > CURRENT_TIMESTAMP;

-- 登录视图，用于GLogin
CREATE VIEW IF NOT EXISTS view_LnR_Verify AS
SELECT Account_Email, Account_Password FROM table_Account;

-- 设置省市选择视图
CREATE VIEW IF NOT EXISTS view_Province_City AS
SELECT 
    CT.City_Province_Code,
    P.Province_Name,
    CT.City_Code,
    CT.City_Name
FROM 
    table_Province P
JOIN 
    table_City CT ON P.Province_Code = CT.City_Province_Code;

-- 影院登录视图
CREATE VIEW IF NOT EXISTS view_Manager_Login AS
SELECT Manage_Account_Email, Manage_Cinema_Province_Code, Manage_Cinema_City_Code, Manage_Cinema_ID
FROM table_Manage;

-- 发行商登录视图
CREATE VIEW IF NOT EXISTS view_Publisher_Login AS
SELECT Publisher_ID, Publisher_Password FROM table_Publisher;

-- 影院管理视图
CREATE VIEW IF NOT EXISTS view_About_Cinema AS
SELECT
    M.Manage_Cinema_Province_Code,
    M.Manage_Cinema_City_Code,
    M.Manage_Cinema_ID,
    C.Cinema_Name,
    C.Cinema_Detailed_Address,
    M.Manage_Account_Email
FROM 
    table_Cinema C
JOIN 
    table_Manage M ON (C.Cinema_Province_Code, C.Cinema_City_Code, C.Cinema_ID) = (M.Manage_Cinema_Province_Code, M.Manage_Cinema_City_Code, M.Manage_Cinema_ID);

-- 用户信息视图
CREATE VIEW IF NOT EXISTS view_Account_Info AS
SELECT Account_Email, Account_Nickname, Account_Gender, Account_Tel, Account_Province_Code, Account_City_Code, Account_SelfIntro, Account_Wallet, Account_CreateTime
FROM table_Account;

-- 合并影厅信息和场次视图，用于Auditoriums页面
CREATE VIEW IF NOT EXISTS view_Auditorium_Schedule AS
SELECT
    -- 影厅基本信息
    Aud.Auditorium_Cinema_Province_Code,
    Aud.Auditorium_Cinema_City_Code,
    Aud.Auditorium_Cinema_ID,
    Aud.Auditorium_ID,
    Aud.Auditorium_Name,
    Aud.Auditorium_Row_Count,
    Aud.Auditorium_Col_Count,
    Aud.Auditorium_Capacity,
    Aud.Visual_Effect_Names,
    -- 排片信息（如果有排片）
    Sch.Schedule_ID,
    Sch.Schedule_Fare,    
    -- 下一场电影信息（如果有排片）
    Sch.Schedule_Film_Publisher_ID,
    Sch.Schedule_Film_ID,
    Sch.Next_Film_Name,
    Sch.Next_Film_Language,
    Sch.Next_Show_Start_Time,
    Sch.Next_Show_End_Time
FROM (
    SELECT
        A.Auditorium_Cinema_Province_Code,
        A.Auditorium_Cinema_City_Code,
        A.Auditorium_Cinema_ID,
        A.Auditorium_ID,
        A.Auditorium_Name,
        A.Auditorium_Row_Count,
        A.Auditorium_Col_Count,
        (A.Auditorium_Row_Count * A.Auditorium_Col_Count) AS Auditorium_Capacity,
        GROUP_CONCAT(DISTINCT VE.Visual_Effect_Name) AS Visual_Effect_Names
    FROM
        table_Auditorium A
    JOIN
        table_Auditorium_Visual_Effects_Association AVEA ON (A.Auditorium_Cinema_Province_Code, A.Auditorium_Cinema_City_Code, A.Auditorium_Cinema_ID, A.Auditorium_ID) = (AVEA.Auditorium_Cinema_Province_Code, AVEA.Auditorium_Cinema_City_Code, AVEA.Auditorium_Cinema_ID, AVEA.Auditorium_ID)
    JOIN
        table_Visual_Effect VE ON AVEA.Auditorium_Visual_Effect = VE.Visual_Effect_Name
    GROUP BY
        A.Auditorium_Cinema_Province_Code, A.Auditorium_Cinema_City_Code, A.Auditorium_Cinema_ID, A.Auditorium_ID
) Aud LEFT JOIN (
    SELECT
        S.Schedule_Cinema_Province_Code,
        S.Schedule_Cinema_City_Code,
        S.Schedule_Cinema_ID,
        S.Schedule_Auditorium_ID,
        S.Schedule_Film_Publisher_ID,
        S.Schedule_Film_ID,
        F.Film_Name AS Next_Film_Name,
        S.Schedule_Film_Language AS Next_Film_Language,
        S.Schedule_Show_Time AS Next_Show_Start_Time,
        (S.Schedule_Show_Time + (F.Film_Duration_Min * INTERVAL '1' MINUTE)) AS Next_Show_End_Time,
        S.Schedule_ID,
        S.Schedule_Fare
    FROM
        table_Schedule S
    JOIN
        table_Film F ON (S.Schedule_Film_Publisher_ID, S.Schedule_Film_ID) = (F.Film_Publisher_ID, F.Film_ID)
    WHERE
        S.Schedule_Show_Time >= CURRENT_TIMESTAMP
) Sch ON (Aud.Auditorium_Cinema_Province_Code, Aud.Auditorium_Cinema_City_Code, Aud.Auditorium_Cinema_ID, Aud.Auditorium_ID) = (Sch.Schedule_Cinema_Province_Code, Sch.Schedule_Cinema_City_Code, Sch.Schedule_Cinema_ID, Sch.Schedule_Auditorium_ID);

-- 播放电影视图，用于PlayMovies
CREATE VIEW IF NOT EXISTS view_Play_Movies AS
SELECT
    Aud.Auditorium_Cinema_Province_Code,
    Aud.Auditorium_Cinema_City_Code,
    Aud.Auditorium_Cinema_ID,
    Aud.Auditorium_ID,
    Aud.Auditorium_Name,
    Aud.Auditorium_Visual_Effect,
    Flm.Film_Publisher_ID,
    Flm.Film_ID,
    Flm.Film_Name,
    Flm.Film_Duration_Min,
    Flm.Film_Min_Fare,
    Flm.Film_Release_Date,
    Flm.Film_Finished_Date,
    Flm.Film_Language_Name,
    Flm.Visual_Effect_Name,
    Flm.Publisher_Name
FROM (
    SELECT
        Au.Auditorium_Cinema_Province_Code,
        Au.Auditorium_Cinema_City_Code,
        Au.Auditorium_Cinema_ID,
        Au.Auditorium_ID,
        Au.Auditorium_Name,
        AVE.Auditorium_Visual_Effect
    FROM
        table_Auditorium Au
    JOIN
        table_Auditorium_Visual_Effects_Association AVE ON (Au.Auditorium_Cinema_Province_Code, Au.Auditorium_Cinema_City_Code, Au.Auditorium_Cinema_ID, Au.Auditorium_ID) = (AVE.Auditorium_Cinema_Province_Code, AVE.Auditorium_Cinema_City_Code, AVE.Auditorium_Cinema_ID, AVE.Auditorium_ID)
) Aud JOIN (
    SELECT
        F.Film_Publisher_ID,
        F.Film_ID,
        F.Film_Name,
        F.Film_Duration_Min,
        F.Film_Min_Fare,
        F.Film_Release_Date,
        F.Film_Finished_Date,
        FLA.Film_Language_Name,
        FVEA.Visual_Effect_Name,
        PUB.Publisher_Name
    FROM
        table_Film F
    JOIN
        table_Film_Languages_Association FLA ON (F.Film_Publisher_ID, F.Film_ID) = (FLA.Film_Publisher_ID, FLA.Film_ID)
    JOIN
        table_Film_Visual_Effects_Association FVEA ON (F.Film_Publisher_ID, F.Film_ID) = (FVEA.Film_Publisher_ID, FVEA.Film_ID)
    JOIN
        table_Publisher PUB ON F.Film_Publisher_ID = PUB.Publisher_ID
) Flm ON Aud.Auditorium_Visual_Effect = Flm.Visual_Effect_Name
WHERE CURRENT_DATE < Flm.Film_Finished_Date;

-- 不可选座位视图，用于Confirm页面
CREATE VIEW IF NOT EXISTS view_Unselectable_Seats AS
SELECT
    Order_Film_Publisher_ID,
    Order_Film_ID,
    Order_Film_Language,
    Order_Visual_Effect,
    Order_Cinema_Province_Code,
    Order_Cinema_City_Code,
    Order_Cinema_ID,
    Order_Auditorium_ID,
    Order_Schedule_ID,
    Order_Row_No,
    Order_Col_No
FROM
    table_Order
WHERE
    Order_Status NOT IN ('C', 'R'); -- 排除已取消、退票的订单

-- Confirm页面详情视图，用于获取影厅信息、电影名、影院名和票价
CREATE VIEW IF NOT EXISTS view_Confirm_Details AS
SELECT
    A.Auditorium_Row_Count,
    A.Auditorium_Col_Count,
    F.Film_Name,
    C.Cinema_Name,
    S.Schedule_Fare,
    S.Schedule_Film_Publisher_ID,
    S.Schedule_Film_ID,
    S.Schedule_Film_Language,
    S.Schedule_Visual_Effect,
    S.Schedule_Cinema_Province_Code,
    S.Schedule_Cinema_City_Code,
    S.Schedule_Cinema_ID,
    S.Schedule_Auditorium_ID,
    S.Schedule_ID
FROM
    table_Auditorium A
JOIN
    table_Schedule S ON (A.Auditorium_Cinema_Province_Code, A.Auditorium_Cinema_City_Code, A.Auditorium_Cinema_ID, A.Auditorium_ID) = 
                        (S.Schedule_Cinema_Province_Code, S.Schedule_Cinema_City_Code, S.Schedule_Cinema_ID, S.Schedule_Auditorium_ID)
JOIN
    table_Film F ON (S.Schedule_Film_Publisher_ID, S.Schedule_Film_ID) = (F.Film_Publisher_ID, F.Film_ID)
JOIN
    table_Cinema C ON (S.Schedule_Cinema_Province_Code, S.Schedule_Cinema_City_Code, S.Schedule_Cinema_ID) = (C.Cinema_Province_Code, C.Cinema_City_Code, C.Cinema_ID);

-- MyOrders页面订单详情视图
CREATE VIEW IF NOT EXISTS view_Order_Details AS
SELECT
    O.Order_Account_Email,
    F.Film_Name,
    C.Cinema_Name,
    S.Schedule_Show_Time,
    S.Schedule_Film_Language,
    S.Schedule_Visual_Effect,
    O.Order_Visual_Effect,
    A.Auditorium_Name,
    O.Order_Row_No,
    O.Order_Col_No,
    S.Schedule_Fare,
    OS.Status_Description,
    O.Order_CreateTime,
    O.Order_Film_Publisher_ID AS Schedule_Film_Publisher_ID,
    O.Order_Film_ID AS Schedule_Film_ID,
    O.Order_Cinema_Province_Code AS Schedule_Cinema_Province_Code,
    O.Order_Cinema_City_Code AS Schedule_Cinema_City_Code,
    O.Order_Cinema_ID AS Schedule_Cinema_ID,
    O.Order_Auditorium_ID AS Schedule_Auditorium_ID,
    O.Order_Schedule_ID AS Schedule_ID
FROM
    table_Order O
JOIN
    table_Order_Status OS ON O.Order_Status = OS.Status_Code
JOIN
    table_Schedule S ON (O.Order_Film_Publisher_ID, O.Order_Film_ID, O.Order_Film_Language, O.Order_Visual_Effect, O.Order_Cinema_Province_Code, O.Order_Cinema_City_Code, O.Order_Cinema_ID, O.Order_Auditorium_ID, O.Order_Schedule_ID) = (S.Schedule_Film_Publisher_ID, S.Schedule_Film_ID, S.Schedule_Film_Language, S.Schedule_Visual_Effect, S.Schedule_Cinema_Province_Code, S.Schedule_Cinema_City_Code, S.Schedule_Cinema_ID, S.Schedule_Auditorium_ID, S.Schedule_ID)
JOIN
    table_Film F ON (O.Order_Film_Publisher_ID = F.Film_Publisher_ID AND O.Order_Film_ID = F.Film_ID)
JOIN
    table_Cinema C ON (O.Order_Cinema_Province_Code = C.Cinema_Province_Code AND O.Order_Cinema_City_Code = C.Cinema_City_Code AND O.Order_Cinema_ID = C.Cinema_ID)
JOIN
    table_Auditorium A ON (O.Order_Cinema_Province_Code = A.Auditorium_Cinema_Province_Code AND O.Order_Cinema_City_Code = A.Auditorium_Cinema_City_Code AND O.Order_Cinema_ID = A.Auditorium_Cinema_ID AND O.Order_Auditorium_ID = A.Auditorium_ID);

-- 影院每天营收总额视图，用于Income页面
CREATE VIEW IF NOT EXISTS view_Cinema_Daily_Income AS
SELECT 
    O.Order_Cinema_Province_Code,
    O.Order_Cinema_City_Code,
    O.Order_Cinema_ID,
    CAST(S.Schedule_Show_Time AS DATE) AS show_date,
    SUM(S.Schedule_Fare) AS daily_income
FROM 
    table_Order O
JOIN 
    table_Schedule S ON O.Order_Schedule_ID = S.Schedule_ID
WHERE
    O.Order_Status NOT IN ('R', 'P', 'C')
GROUP BY 
    O.Order_Cinema_Province_Code,
    O.Order_Cinema_City_Code,
    O.Order_Cinema_ID,
    CAST(S.Schedule_Show_Time AS DATE);

-- 影院每月营收总额视图，用于Income页面
CREATE VIEW IF NOT EXISTS view_Cinema_Monthly_Income AS
SELECT 
    Order_Cinema_Province_Code,
    Order_Cinema_City_Code,
    Order_Cinema_ID,
    YEAR(show_date) AS show_year,
    MONTH(show_date) AS show_month,
    SUM(daily_income) AS monthly_income
FROM 
    view_Cinema_Daily_Income
GROUP BY 
    Order_Cinema_Province_Code,
    Order_Cinema_City_Code,
    Order_Cinema_ID,
    YEAR(show_date),
    MONTH(show_date);