CREATE OR REPLACE PACKAGE BODY pkg_inventario AS

    -- [PROCEDIMIENTO PRIVADO] 
    -- Solo puede ser llamado desde dentro de este body.
    -- Verifica si hay stock suficiente antes de una venta.
    FUNCTION fn_verificar_stock(p_id_prod NUMBER, p_cantidad NUMBER) RETURN BOOLEAN IS
        v_stock_actual NUMBER;
    BEGIN
        SELECT stock INTO v_stock_actual 
        FROM productos 
        WHERE id_producto = p_id_prod;

        RETURN v_stock_actual >= p_cantidad;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RETURN FALSE;
    END fn_verificar_stock;


    -- [PROCEDIMIENTO PÚBLICO]
    -- Registra una salida de producto (venta)
    PROCEDURE registrar_salida(p_id_prod NUMBER, p_cantidad NUMBER) IS
    BEGIN
        IF fn_verificar_stock(p_id_prod, p_cantidad) THEN
            UPDATE productos 
            SET stock = stock - p_cantidad,
                ultima_actualizacion = SYSDATE
            WHERE id_producto = p_id_prod;
            
            DBMS_OUTPUT.PUT_LINE('Salida registrada con éxito.');
        ELSE
            RAISE_APPLICATION_ERROR(-20001, 'Stock insuficiente para el producto: ' || p_id_prod);
        END IF;
    END registrar_salida;


    -- [PROCEDIMIENTO PÚBLICO]
    -- Repone stock de un producto
    PROCEDURE reponer_stock(p_id_prod NUMBER, p_cantidad NUMBER) IS
    BEGIN
        UPDATE productos 
        SET stock = stock + p_cantidad,
            ultima_actualizacion = SYSDATE
        WHERE id_producto = p_id_prod;
    END reponer_stock;

END pkg_inventario;
/
