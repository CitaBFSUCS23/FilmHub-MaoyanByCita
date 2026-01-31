-- HSQLDB 基本表

-- 国籍表 ISO 3166-1 国家代码
CREATE TABLE IF NOT EXISTS table_Nationality (
    Nationality_Name CHAR(3) PRIMARY KEY,      -- 国籍名称（3位大写英文）
);

-- 发行商表
CREATE TABLE IF NOT EXISTS table_Publisher (
    Publisher_ID NUMERIC(6) PRIMARY KEY,        -- 发行商ID（6位数字，由平台指派）
    Publisher_Password VARCHAR(20) NOT NULL,    -- 发行商密码（最多20位字符）
    Publisher_Name CHAR(20) NOT NULL,           -- 发行商名称（最多20位字符）
    Publisher_Nationality CHAR(3) REFERENCES table_Nationality(Nationality_Name) ON DELETE RESTRICT,    -- 发行商国籍（3位大写英文，引用table_Nationality表）
);

-- 影片类型表
CREATE TABLE IF NOT EXISTS table_Film_Type (
    Film_Type_Name CHAR(20) PRIMARY KEY          -- 影片类型名称，已存入数据库（最多20位字符）
);

-- 影片语言表
CREATE TABLE IF NOT EXISTS table_Film_Language (
    Film_Language_Name CHAR(20) PRIMARY KEY      -- 影片语言名称，已存入数据库（最多20位字符）
);

-- 视觉效果表
CREATE TABLE IF NOT EXISTS table_Visual_Effect (
    Visual_Effect_Name CHAR(20) PRIMARY KEY      -- 视觉效果名称，已存入数据库（最多20位字符）
);

-- 影片表
CREATE TABLE IF NOT EXISTS table_Film (
    Film_Publisher_ID NUMERIC(6) REFERENCES table_Publisher(Publisher_ID) ON DELETE RESTRICT, -- 发行商ID（6位数字，引用table_Publisher表）
    Film_ID NUMERIC(6), -- 影片ID（6位数字，自动生成，不同发行商可以有相同的Film_ID，对于同一个发行商，Film_ID是唯一的）
    Film_Name CHAR(20) NOT NULL, -- 影片名称（最多20位字符）
    Film_Publish_Date DATE DEFAULT CURRENT_DATE, -- 影片发布日期（默认当前日期）
    Film_Release_Date DATE NOT NULL, -- 影片上映日期（由发行商确定）
    Film_Finished_Date DATE NOT NULL, -- 影片结映日期（由发行商确定）
    Film_Intro CHAR(200) DEFAULT NULL, -- 影片简介（最多200位字符，由发行商确定）
    Film_Duration_Min NUMERIC(3) CHECK (Film_Duration_Min > 0), -- 影片时长（分钟，正整数，由发行商确定）
    Film_Min_Fare REAL CHECK (Film_Min_Fare >= 0), -- 影片最低票价（非负实数，由发行商确定）
    Film_Box_Office REAL DEFAULT 0.00, -- 影片票房（默认0.00）
    PRIMARY KEY (Film_Publisher_ID, Film_ID) -- 发行商ID和影片ID联合主键
);

-- 影片类型关联表
CREATE TABLE IF NOT EXISTS table_Film_Types_Association (
    Film_Publisher_ID NUMERIC(6), -- 发行商ID（6位数字，引用table_Publisher表）
    Film_ID NUMERIC(6), -- 影片ID（6位数字）
    Film_Type_Name CHAR(20) REFERENCES table_Film_Type(Film_Type_Name), -- 影片类型名称（最多20位字符，引用table_Film_Type表）
    PRIMARY KEY (Film_Publisher_ID, Film_ID, Film_Type_Name), -- 联合主键
    FOREIGN KEY (Film_Publisher_ID, Film_ID) REFERENCES table_Film(Film_Publisher_ID, Film_ID) ON DELETE CASCADE -- 引用table_Film表的联合主键
);

-- 影片语言关联表
CREATE TABLE IF NOT EXISTS table_Film_Languages_Association (
    Film_Publisher_ID NUMERIC(6), -- 发行商ID（6位数字，引用table_Publisher表）
    Film_ID NUMERIC(6), -- 影片ID（6位数字）
    Film_Language_Name CHAR(20) REFERENCES table_Film_Language(Film_Language_Name), -- 影片语言名称（最多20位字符，引用table_Film_Language表）
    PRIMARY KEY (Film_Publisher_ID, Film_ID, Film_Language_Name), -- 联合主键
    FOREIGN KEY (Film_Publisher_ID, Film_ID) REFERENCES table_Film(Film_Publisher_ID, Film_ID) ON DELETE CASCADE -- 引用table_Film表的联合主键
);

-- 影片视觉效果关联表
CREATE TABLE IF NOT EXISTS table_Film_Visual_Effects_Association (
    Film_Publisher_ID NUMERIC(6), -- 发行商ID（6位数字，引用table_Publisher表）
    Film_ID NUMERIC(6), -- 影片ID（6位数字）
    Visual_Effect_Name CHAR(20) REFERENCES table_Visual_Effect(Visual_Effect_Name), -- 视觉效果名称（最多20位字符，引用table_Visual_Effect表）
    PRIMARY KEY (Film_Publisher_ID, Film_ID, Visual_Effect_Name), -- 联合主键
    FOREIGN KEY (Film_Publisher_ID, Film_ID) REFERENCES table_Film(Film_Publisher_ID, Film_ID) ON DELETE CASCADE -- 引用table_Film表的联合主键
);

-- 省份代码和名称对应表
CREATE TABLE IF NOT EXISTS table_Province (
    Province_Code NUMERIC(2) PRIMARY KEY, -- 省份代码，已存入数据库（2位数字）
    Province_Name VARCHAR(20) UNIQUE -- 省份名称，已存入数据库（最多20位字符，唯一）
);

-- 城市代码和名称对应表
CREATE TABLE IF NOT EXISTS table_City (
    City_Province_Code NUMERIC(2) REFERENCES table_Province(Province_Code) ON DELETE RESTRICT, -- 省份代码，已存入数据库（2位数字，引用table_Province表）
    City_Code NUMERIC(2), -- 城市代码，已存入数据库（2位数字）
    City_Name VARCHAR(20) UNIQUE, -- 城市名称，已存入数据库（最多20位字符，唯一）
    PRIMARY KEY (City_Province_Code, City_Code) -- 省份代码和城市代码联合主键
);

-- 影院表
CREATE TABLE IF NOT EXISTS table_Cinema (
    Cinema_Province_Code NUMERIC(2), -- 省份代码，已存入数据库（2位数字，引用table_Province表）
    Cinema_City_Code NUMERIC(2), -- 城市代码，已存入数据库（2位数字，引用table_City表）
    Cinema_ID NUMERIC(4), -- 影院ID（4位数字，自动生成，不同省份、不同城市可以有相同的Cinema_ID，对于同一个省份、城市，Cinema_ID是唯一的）
    Cinema_Detailed_Address CHAR(100) NOT NULL, -- 影院详细地址（最多100位字符）
    Cinema_Name CHAR(20) NOT NULL, -- 影院名称（最多20位字符）
    PRIMARY KEY (Cinema_Province_Code, Cinema_City_Code, Cinema_ID),
    FOREIGN KEY (Cinema_Province_Code, Cinema_City_Code) REFERENCES table_City(City_Province_Code, City_Code) ON DELETE RESTRICT
);

-- 影厅表
CREATE TABLE IF NOT EXISTS table_Auditorium (
    Auditorium_Cinema_Province_Code NUMERIC(2), -- 省份代码，已存入数据库（2位数字，引用table_Province表）
    Auditorium_Cinema_City_Code NUMERIC(2), -- 城市代码，已存入数据库（2位数字，引用table_City表）
    Auditorium_Cinema_ID NUMERIC(4), -- 影院ID（4位数字，引用table_Cinema表）
    Auditorium_ID NUMERIC(3), -- 影厅ID（3位数字，自动生成，不同影院可以有相同的Auditorium_ID，对于同一个影院，Auditorium_ID是唯一的）
    Auditorium_Name CHAR(20) DEFAULT 'Common Auditorium', -- 影厅名称（最多20位字符）
    Auditorium_Row_Count NUMERIC(2) CHECK (Auditorium_Row_Count > 0 AND Auditorium_Row_Count <= 99), -- 影厅座位行数（正整数）
    Auditorium_Col_Count NUMERIC(2) CHECK (Auditorium_Col_Count > 0 AND Auditorium_Col_Count <= 99), -- 影厅座位列数（正整数）
    PRIMARY KEY (Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID, Auditorium_ID),
    FOREIGN KEY (Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID) 
        REFERENCES table_Cinema(Cinema_Province_Code, Cinema_City_Code, Cinema_ID) ON DELETE RESTRICT
);

-- 影厅视觉效果关联表
CREATE TABLE IF NOT EXISTS table_Auditorium_Visual_Effects_Association (
    Auditorium_Cinema_Province_Code NUMERIC(2), -- 省份代码，已存入数据库（2位数字，引用table_Province表）
    Auditorium_Cinema_City_Code NUMERIC(2), -- 城市代码，已存入数据库（2位数字，引用table_City表）
    Auditorium_Cinema_ID NUMERIC(4), -- 影院ID（4位数字，引用table_Cinema表）
    Auditorium_ID NUMERIC(3), -- 影厅ID（3位数字，引用table_Auditorium表）
    Auditorium_Visual_Effect CHAR(20) REFERENCES table_Visual_Effect(Visual_Effect_Name), -- 视觉效果名称（最多20位字符，引用table_Visual_Effect表）
    PRIMARY KEY (Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID, Auditorium_ID, Auditorium_Visual_Effect), -- 联合主键
    FOREIGN KEY (Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID, Auditorium_ID) 
        REFERENCES table_Auditorium(Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID, Auditorium_ID) ON DELETE CASCADE
);

-- 排片表
CREATE TABLE IF NOT EXISTS table_Schedule (
    Schedule_Film_Publisher_ID NUMERIC(6), -- 发行商ID（6位数字，引用table_Film_Language表）
    Schedule_Film_ID NUMERIC(6), -- 影片ID（6位数字，引用table_Film_Language表）
    Schedule_Film_Language CHAR(20),  -- 影片语言名称（最多20位字符，引用table_Film_Language表）
    Schedule_Visual_Effect CHAR(20), -- 视觉效果名称（最多20位字符，引用table_Visual_Effect表）
    Schedule_Cinema_Province_Code NUMERIC(2), -- 省份代码，已存入数据库（2位数字，引用table_Province表）
    Schedule_Cinema_City_Code NUMERIC(2), -- 城市代码，已存入数据库（2位数字，引用table_City表）
    Schedule_Cinema_ID NUMERIC(4), -- 影院ID（4位数字，引用table_Cinema表）
    Schedule_Auditorium_ID NUMERIC(3), -- 影厅ID（3位数字，引用table_Auditorium表）
    Schedule_ID NUMERIC(3), -- 排片ID（3位数字，自动生成，可以有相同的Schedule_ID，对于同一个影厅，Schedule_ID是唯一的）
    Schedule_Fare REAL CHECK (Schedule_Fare >= 0), -- 排片票价（非负实数，大于等于影片最低票价）
    Schedule_Show_Time TIMESTAMP NOT NULL, -- 放映时间（由影院确定）
    PRIMARY KEY (Schedule_Film_Publisher_ID, Schedule_Film_ID, Schedule_Film_Language, Schedule_Visual_Effect, Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, Schedule_Cinema_ID, Schedule_Auditorium_ID, Schedule_ID), -- 联合主键
    FOREIGN KEY (Schedule_Film_Publisher_ID, Schedule_Film_ID, Schedule_Film_Language) 
        REFERENCES table_Film_Languages_Association(Film_Publisher_ID, Film_ID, Film_Language_Name) ON DELETE RESTRICT, -- 确保放映的影片语言与发行商的影片语言一致
    FOREIGN KEY (Schedule_Film_Publisher_ID, Schedule_Film_ID, Schedule_Visual_Effect) 
        REFERENCES table_Film_Visual_Effects_Association(Film_Publisher_ID, Film_ID, Visual_Effect_Name) ON DELETE RESTRICT, -- 确保放映的视觉效果与发行商的影片视觉效果一致
    FOREIGN KEY (Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, Schedule_Cinema_ID, Schedule_Auditorium_ID) 
        REFERENCES table_Auditorium(Auditorium_Cinema_Province_Code, Auditorium_Cinema_City_Code, Auditorium_Cinema_ID, Auditorium_ID) ON DELETE RESTRICT
);

-- 基本账户表
CREATE TABLE IF NOT EXISTS table_Account (
    Account_Email VARCHAR(50) PRIMARY KEY, -- 账户邮箱（最多50位字符，唯一）
    Account_Nickname VARCHAR(20) NOT NULL, -- 账户昵称（最多20位字符）
    Account_Password CHAR(20) NOT NULL, -- 账户密码（最多20位字符）
    Account_Gender CHAR(1) DEFAULT 'U' CHECK (Account_Gender IN ('M', 'F', 'U')), -- 账户性别（默认Unkown. 可填M或F）
    Account_Tel VARCHAR(20) DEFAULT NULL, -- 账户手机号（最多20位字符，默认NULL）
    Account_Province_Code NUMERIC(2), -- 省份代码，已存入数据库（2位数字，引用table_Province表）
    Account_City_Code NUMERIC(2), -- 城市代码，已存入数据库（2位数字，引用table_City表）
    Account_SelfIntro VARCHAR(200) DEFAULT NULL, -- 账户自我介绍（最多200位字符，默认NULL）
    Account_Wallet REAL DEFAULT 0.00, -- 账户钱包余额（默认0.00）
    Account_CreateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 账户创建时间（默认当前时间）
    FOREIGN KEY (Account_Province_Code, Account_City_Code) REFERENCES table_City(City_Province_Code, City_Code) ON DELETE RESTRICT
);

-- 订单状态码定义表
CREATE TABLE IF NOT EXISTS table_Order_Status (
    Status_Code CHAR(1) PRIMARY KEY, -- 订单状态码，已存入数据库（1位字符）
    Status_Description VARCHAR(20) NOT NULL -- 订单状态描述，已存入数据库（最多20位字符）
);

-- 订单状态码转换表
CREATE TABLE IF NOT EXISTS table_Order_Status_Conversion (
    Conversion_From CHAR(1) REFERENCES table_Order_Status(Status_Code) ON DELETE RESTRICT, -- 合法的状态转化起点，已存入数据库
    Conversion_To CHAR(1) REFERENCES table_Order_Status(Status_Code) ON DELETE RESTRICT,   -- 合法的状态转化终点，已存入数据库
    PRIMARY KEY (Conversion_From, Conversion_To)
);

-- 订单号
CREATE TABLE IF NOT EXISTS table_Order (
    Order_Film_Publisher_ID NUMERIC(6), -- 发行商ID（6位数字，引用table_Publisher表）
    Order_Film_ID NUMERIC(6), -- 影片ID（6位数字，引用table_Film表）
    Order_Film_Language CHAR(20), -- 影片语言名称（最多20位字符，引用table_Schedule表）
    Order_Visual_Effect CHAR(20), -- 视觉效果名称（最多20位字符，引用table_Visual_Effect表）
    Order_Cinema_Province_Code NUMERIC(2), -- 省份代码，已存入数据库（2位数字，引用table_Province表）
    Order_Cinema_City_Code NUMERIC(2), -- 城市代码，已存入数据库（2位数字，引用table_City表）
    Order_Cinema_ID NUMERIC(4), -- 影院ID（4位数字，引用table_Cinema表）
    Order_Auditorium_ID NUMERIC(3), -- 影厅ID（3位数字，引用table_Auditorium表）
    Order_Schedule_ID NUMERIC(3), -- 排片ID（3位数字，引用table_Schedule表）
    Order_Row_No NUMERIC(2), -- 座位行号（3位数字）
    Order_Col_No NUMERIC(2), -- 座位列号（3位数字）
    Order_Account_Email VARCHAR(50) REFERENCES table_Account(Account_Email) ON DELETE CASCADE,     -- 订单关联的账户邮箱
    Order_Status CHAR(1) REFERENCES table_Order_Status(Status_Code) ON DELETE RESTRICT, -- 订单状态码
    Order_CreateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 订单创建时间（默认当前时间）
    Order_LastUpdateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP CHECK (Order_LastUpdateTime >= Order_CreateTime), -- 订单最后更新时间（默认当前时间，每次更新自动设置为当前时间）
    PRIMARY KEY (Order_Film_Publisher_ID, Order_Film_ID, Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, Order_Auditorium_ID, Order_Schedule_ID, Order_Row_No, Order_Col_No, Order_CreateTime), -- 联合主键
    FOREIGN KEY (Order_Film_Publisher_ID, Order_Film_ID, Order_Film_Language, Order_Visual_Effect, Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, Order_Auditorium_ID, Order_Schedule_ID) 
        REFERENCES table_Schedule(Schedule_Film_Publisher_ID, Schedule_Film_ID, Schedule_Film_Language, Schedule_Visual_Effect, Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, Schedule_Cinema_ID, Schedule_Auditorium_ID, Schedule_ID) ON DELETE RESTRICT -- 确保场次有效
);

-- 管理影院表
CREATE TABLE IF NOT EXISTS table_Manage (
    Manage_Account_Email VARCHAR(50) REFERENCES table_Account(Account_Email) ON DELETE CASCADE, -- 管理账户邮箱，已存入数据库（引用table_Account表）
    Manage_Cinema_Province_Code NUMERIC(2), -- 省份代码，已存入数据库（2位数字，引用table_Province表）
    Manage_Cinema_City_Code NUMERIC(2), -- 城市代码，已存入数据库（2位数字，引用table_City表）
    Manage_Cinema_ID NUMERIC(4), -- 影院ID（4位数字，引用table_Cinema表）
    Manage_GrantTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 授权时间（默认当前时间）
    PRIMARY KEY (Manage_Account_Email, Manage_Cinema_Province_Code, Manage_Cinema_City_Code, Manage_Cinema_ID),
    FOREIGN KEY (Manage_Cinema_Province_Code, Manage_Cinema_City_Code, Manage_Cinema_ID) 
        REFERENCES table_Cinema(Cinema_Province_Code, Cinema_City_Code, Cinema_ID) ON DELETE RESTRICT
);