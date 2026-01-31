-- HSQLDB 触发器

-- 设置触发器，检查排片时间冲突
CREATE TRIGGER IF NOT EXISTS trigger_Schedule_Time_Conflict
BEFORE INSERT ON table_Schedule
REFERENCING NEW AS NEW
FOR EACH ROW
BEGIN ATOMIC
    -- 检查同一影厅在同一时间是否有排片冲突
    DECLARE film_duration_min NUMERIC(3);
    DECLARE new_end_time TIMESTAMP;
    
    -- 获取影片时长
    SELECT Film_Duration_Min INTO film_duration_min 
    FROM table_Film 
    WHERE (Film_Publisher_ID, Film_ID) = (NEW.Schedule_Film_Publisher_ID, New.Schedule_Film_ID);
    
    -- 计算新排片的结束时间
    SET new_end_time = TIMESTAMPADD(MINUTE, film_duration_min, NEW.Schedule_Show_Time);
    
    -- 检查是否存在时间冲突的排片
    IF EXISTS (
        SELECT 1 FROM table_Schedule 
        WHERE (Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, Schedule_Cinema_ID, Schedule_Auditorium_ID) = (NEW.Schedule_Cinema_Province_Code, NEW.Schedule_Cinema_City_Code, NEW.Schedule_Cinema_ID, NEW.Schedule_Auditorium_ID)
        AND (
                -- 新排片开始时间在已有排片的时间段内
            (
                NEW.Schedule_Show_Time >= Schedule_Show_Time 
                AND 
                NEW.Schedule_Show_Time < TIMESTAMPADD(MINUTE, (
                    SELECT Film_Duration_Min FROM table_Film WHERE (Film_Publisher_ID, Film_ID) = (Schedule_Film_Publisher_ID, Schedule_Film_ID)
                ), Schedule_Show_Time)
            ) OR (
                -- 新排片结束时间在已有排片的时间段内
                new_end_time > Schedule_Show_Time 
                AND 
                new_end_time <= TIMESTAMPADD(MINUTE, (
                    SELECT Film_Duration_Min FROM table_Film WHERE (Film_Publisher_ID, Film_ID) = (Schedule_Film_Publisher_ID,Schedule_Film_ID)
                ), Schedule_Show_Time)
            ) OR (
                -- 已有排片开始时间在新排片的时间段内
                Schedule_Show_Time >= NEW.Schedule_Show_Time 
                AND 
                Schedule_Show_Time < new_end_time
            )
        )
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '排片时间冲突：该影厅在指定时间段内已有其他排片';
    END IF;
END;

-- 设置触发器，确保同一排片和座位上只有一个有效订单（状态不是'C'取消或'R'退票）
CREATE TRIGGER IF NOT EXISTS trigger_Order_Seat_Conflict
BEFORE INSERT ON table_Order
REFERENCING NEW AS NEW
FOR EACH ROW
-- 只在订单状态不是取消或退票时检查冲突
WHEN (NEW.Order_Status NOT IN ('C', 'R'))
BEGIN ATOMIC
    -- 检查同一排片和座位上是否存在有效订单
    IF EXISTS (
        SELECT 1 FROM table_Order 
        WHERE (Order_Schedule_ID, Order_Row_No, Order_Col_No) = (NEW.Order_Schedule_ID, NEW.Order_Row_No, NEW.Order_Col_No)
        AND Order_Status NOT IN ('C', 'R') -- 只检查有效订单（非取消、非退票）
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '座位已被抢占，请刷新页面重试';
    END IF;
END;

-- 设置触发器，检查排片票价是否大于等于影片最低票价
CREATE TRIGGER IF NOT EXISTS trigger_Schedule_Minimun_Fare
BEFORE INSERT ON table_Schedule
REFERENCING NEW AS NEW
FOR EACH ROW
BEGIN ATOMIC
    -- 检查排片票价是否大于等于对应影片的最低票价
    DECLARE min_fare REAL;
    SELECT Film_Min_Fare INTO min_fare 
    FROM table_Film 
    WHERE (Film_Publisher_ID, Film_ID) = (NEW.Schedule_Film_Publisher_ID, NEW.Schedule_Film_ID);
    
    IF (NEW.Schedule_Fare < min_fare) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '排片票价不得低于影片最低票价';
    END IF;
END;

-- 设置触发器，检查订单状态转化是否合法；并更新影片票房。注意：订单支付的金额应当从排片表中获得
-- 订单从"未付P"到"已付D"时触发增量，从"已付D"到"退票R"时触发减量，其他状态转换不触发票房更新
CREATE TRIGGER IF NOT EXISTS trigger_Order_Status_Conversion
AFTER UPDATE ON table_Order
REFERENCING 
    NEW AS NEW 
    OLD AS OLD
FOR EACH ROW
WHEN (NEW.Order_Status != OLD.Order_Status)
BEGIN ATOMIC
    DECLARE schedule_fare REAL;
    -- 检查状态转换是否在合法转换表中
    IF NOT EXISTS (
        SELECT 1 FROM table_Order_Status_Conversion 
        WHERE (Conversion_From, Conversion_To) = (OLD.Order_Status, NEW.Order_Status) 
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '订单状态转换不合法';
    ELSE 
        -- 从排片表获取票价信息
        SELECT Schedule_Fare INTO schedule_fare 
        FROM table_Schedule 
        WHERE (Schedule_Film_Publisher_ID, Schedule_Film_ID, Schedule_Film_Language, Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, Schedule_Cinema_ID, Schedule_Auditorium_ID, Schedule_ID) = (NEW.Order_Film_Publisher_ID, NEW.Order_Film_ID, NEW.Order_Film_Language, NEW.Order_Cinema_Province_Code, NEW.Order_Cinema_City_Code, NEW.Order_Cinema_ID, NEW.Order_Auditorium_ID, NEW.Order_Schedule_ID);
        
        -- 根据状态转换更新影片票房
        IF ((OLD.Order_Status, NEW.Order_Status) = ('P', 'D')) THEN
            -- 订单从"未付P"到"已付D"时，更新影片票房增量
            UPDATE table_Film 
            SET Film_Box_Office = Film_Box_Office + schedule_fare
            WHERE (Film_Publisher_ID, Film_ID) = (NEW.Order_Film_Publisher_ID, NEW.Order_Film_ID);
        ELSEIF ((OLD.Order_Status, NEW.Order_Status) = ('D', 'R')) THEN
            -- 订单从"已付D"到"退票R"时，更新影片票房减量
            UPDATE table_Film 
            SET Film_Box_Office = Film_Box_Office - schedule_fare
            WHERE (Film_Publisher_ID, Film_ID) = (NEW.Order_Film_Publisher_ID, NEW.Order_Film_ID);
        END IF;
    END IF;
END;

-- 设置触发器，限制用户在一个放映场次上最多购买6张票
CREATE TRIGGER IF NOT EXISTS trigger_Order_Ticket_Limit
BEFORE INSERT ON table_Order
REFERENCING NEW AS NEW
FOR EACH ROW
-- 只在订单状态不是取消或退票时检查限制
WHEN (NEW.Order_Status NOT IN ('C', 'R'))
BEGIN ATOMIC
    IF (
        SELECT COUNT(1)
        FROM table_Order
        WHERE (Order_Film_Publisher_ID, Order_Film_ID, Order_Film_Language, Order_Visual_Effect, Order_Cinema_Province_Code, Order_Cinema_City_Code, Order_Cinema_ID, Order_Auditorium_ID, Order_Schedule_ID) = (NEW.Order_Film_Publisher_ID, NEW.Order_Film_ID, NEW.Order_Film_Language, NEW.Order_Visual_Effect, NEW.Order_Cinema_Province_Code, NEW.Order_Cinema_City_Code, NEW.Order_Cinema_ID, NEW.Order_Auditorium_ID, NEW.Order_Schedule_ID)
        AND Order_Account_Email = NEW.Order_Account_Email
        AND Order_Status NOT IN ('C', 'R')
    ) >= 6 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '每个放映场次最多只能购买6张票';
    END IF;
END;

-- 设置触发器，检查用户钱包余额是否足够，并在订单状态变更时更新余额。订单从"未付P"到"已付D"时扣款，从"已付D"到"退票R"时退款
CREATE TRIGGER IF NOT EXISTS trigger_Order_Wallet_Balance
AFTER UPDATE ON table_Order
REFERENCING 
    NEW AS NEW 
    OLD AS OLD
FOR EACH ROW
WHEN (NEW.Order_Status != OLD.Order_Status)
BEGIN ATOMIC
    DECLARE schedule_fare REAL;
    DECLARE account_wallet REAL;
    DECLARE schedule_show_time TIMESTAMP;
    
    -- 从排片表获取票价和放映时间信息
    SELECT Schedule_Fare, Schedule_Show_Time INTO schedule_fare, schedule_show_time 
    FROM table_Schedule 
    WHERE (Schedule_Film_Publisher_ID, Schedule_Film_ID, Schedule_Film_Language, Schedule_Cinema_Province_Code, Schedule_Cinema_City_Code, Schedule_Cinema_ID, Schedule_Auditorium_ID, Schedule_ID) = (NEW.Order_Film_Publisher_ID, NEW.Order_Film_ID, NEW.Order_Film_Language, NEW.Order_Cinema_Province_Code, NEW.Order_Cinema_City_Code, NEW.Order_Cinema_ID, NEW.Order_Auditorium_ID, NEW.Order_Schedule_ID);
    
    -- 根据状态转换更新用户钱包余额
    IF ((OLD.Order_Status, NEW.Order_Status) = ('P', 'D')) THEN
        -- 订单从"未付P"到"已付D"时，检查并扣除用户钱包余额
        SELECT Account_Wallet INTO account_wallet 
        FROM table_Account 
        WHERE Account_Email = NEW.Order_Account_Email;
        
        IF (account_wallet < schedule_fare) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '支付失败：钱包余额不足。请在个人中心充值后重试';
        END IF;
        
        -- 扣除钱包余额
        UPDATE table_Account 
        SET Account_Wallet = Account_Wallet - schedule_fare
        WHERE Account_Email = NEW.Order_Account_Email;
        
    ELSEIF ((OLD.Order_Status, NEW.Order_Status) = ('D', 'R')) THEN
        -- 订单从"已付D"到"退票R"时，检查距离开场时间是否小于10分钟
        IF (CURRENT_TIMESTAMP > TIMESTAMPADD(MINUTE, -10, schedule_show_time)) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '退票失败：距离开场时间不足10分钟，无法退票';
        END IF;
        
        -- 将扣除的金额加回到用户钱包
        UPDATE table_Account 
        SET Account_Wallet = Account_Wallet + schedule_fare
        WHERE Account_Email = NEW.Order_Account_Email;
    END IF;
END;