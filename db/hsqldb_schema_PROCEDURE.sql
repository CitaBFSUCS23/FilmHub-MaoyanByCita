-- HSQLDB 存储过程

-- 创建存储过程，用于自动取消超时未支付的订单
CREATE PROCEDURE procedure_Shift_Unpaid_Orders()
MODIFIES SQL DATA
BEGIN ATOMIC
    -- 更新所有状态为'P'（未支付）且创建时间超过10分钟的订单，将它们的状态更新为'C'（取消）
    UPDATE table_Order 
    SET Order_Status = 'C'
    WHERE Order_Status = 'P' 
    AND Order_CreateTime < TIMESTAMPADD(MINUTE, -10, CURRENT_TIMESTAMP);
END;

-- 创建存储过程，用于自动将放映结束的订单状态更新为'完成'
CREATE PROCEDURE procedure_Shift_Completed_Orders()
MODIFIES SQL DATA
BEGIN ATOMIC
    -- 更新所有已结束放映且状态不是'P'（未支付）、'C'（取消）、'R'（退票）的订单，将它们的状态更新为'F'（完成）
    UPDATE table_Order O
    SET O.Order_Status = 'F'
    WHERE O.Order_Status NOT IN ('P', 'C', 'R')
    AND EXISTS (
        SELECT 1
        FROM table_Schedule S
        JOIN table_Film F ON (S.Schedule_Film_Publisher_ID, S.Schedule_Film_ID) = (F.Film_Publisher_ID, F.Film_ID)
        WHERE (S.Schedule_Film_Publisher_ID, S.Schedule_Film_ID, S.Schedule_Film_Language, S.Schedule_Visual_Effect, S.Schedule_Cinema_Province_Code, S.Schedule_Cinema_City_Code, S.Schedule_Cinema_ID, S.Schedule_Auditorium_ID, S.Schedule_ID) = (O.Order_Film_Publisher_ID, O.Order_Film_ID, O.Order_Film_Language, O.Order_Visual_Effect, O.Order_Cinema_Province_Code, O.Order_Cinema_City_Code, O.Order_Cinema_ID, O.Order_Auditorium_ID, O.Order_Schedule_ID)
        AND TIMESTAMPADD(MINUTE, F.Film_Duration_Min, S.Schedule_Show_Time) < CURRENT_TIMESTAMP
    );
END;