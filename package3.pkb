CREATE OR REPLACE PACKAGE BODY pkg_logistica AS

    -- Constante privada (solo visible en este body)
    c_estado_pendiente CONSTANT VARCHAR2(20) := 'PENDIENTE';

    -- [PROCEDIMIENTO] Asignar repartidor a todos los pedidos pendientes
    PROCEDURE asignar_repartidores_masivo(p_id_repartidor NUMBER) IS
        -- Cursor para recorrer pedidos que no tienen transportista
        CURSOR cur_pedidos_libres IS
            SELECT id_pedido 
            FROM pedidos 
            WHERE estado = c_estado_pendiente 
            AND id_repartidor IS NULL;
            
        v_conteo NUMBER := 0;
    BEGIN
        FOR r_pedido IN cur_pedidos_libres LOOP
            UPDATE pedidos 
            SET id_repartidor = p_id_repartidor,
                estado = 'EN RUTA',
                fecha_asignacion = SYSDATE
            WHERE id_pedido = r_pedido.id_pedido;
            
            v_conteo := v_conteo + 1;
        END LOOP;

        DBMS_OUTPUT.PUT_LINE('Se han asignado ' || v_conteo || ' pedidos.');
    END asignar_repartidores_masivo;

    -- [FUNCIÓN] Calcular costo de envío basado en distancia
    FUNCTION calcular_costo_envio(p_distancia_km NUMBER) RETURN NUMBER IS
        v_tarifa_base NUMBER := 5.00;
        v_precio_km   NUMBER := 0.50;
    BEGIN
        IF p_distancia_km < 0 THEN
            RETURN 0;
        END IF;
        
        RETURN v_tarifa_base + (p_distancia_km * v_precio_km);
    END calcular_costo_envio;

END pkg_logistica;
/
