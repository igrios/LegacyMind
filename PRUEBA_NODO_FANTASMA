CREATE OR REPLACE PACKAGE BODY PKG_FACTURACION AS

    PROCEDURE SP_PROCESAR_PAGO (P_CLIENTE_ID IN NUMBER, P_MONTO IN NUMBER) IS
    BEGIN
        -- Un select básico con JOIN estándar
        SELECT c.nombre, f.nro_factura
        FROM CLIENTES c
        JOIN FACTURAS f ON c.id = f.cliente_id
        WHERE c.id = P_CLIENTE_ID;

        -- Un update simple
        UPDATE CUENTAS_CORRIENTES
        SET saldo = saldo - P_MONTO
        WHERE cliente_id = P_CLIENTE_ID;

        -- Un insert simple
        INSERT INTO AUDITORIA_PAGOS (id, fecha, monto)
        VALUES (SEQ_AUDIT.NEXTVAL, SYSDATE, P_MONTO);
        
        COMMIT;
    END SP_PROCESAR_PAGO;

END PKG_FACTURACION;

CREATE OR REPLACE 
PACKAGE BODY          PKG_LOGISTICA 
AS
    PROCEDURE SP_DESPACHAR_STOCK IS
    BEGIN
        /* Este comentario multilínea
           suele romper parsers si no se limpia bien */
        INSERT 
        INTO 
        HISTORIAL_REPARTO (id, tracking) -- Comentario de línea pegado
        SELECT seq.NEXTVAL, e.codigo
        FROM 
            ENVIO e 
            INNER JOIN DETALLE_ENVIO d 
            ON e.id = d.envio_id;
            
    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
    END SP_DESPACHAR_STOCK;
END;

CREATE OR REPLACE PROCEDURE SP_REPORTE_PRODUCTOS IS
BEGIN
    -- Query con Joins implícitos por coma (Estilo Oracle antiguo)
    SELECT p.descripcion, c.CATEGORIA_NOM, s.cantidad
    FROM PRODUCTOS p, CATEGORIAS c, STOCK_DEPOSITO s
    WHERE p.categoria_id = c.id
      AND p.id = s.producto_id(+); -- El (+) indica un Left Join en Oracle
END;

CREATE OR REPLACE PROCEDURE SP_CLIENTES_TOP IS
BEGIN
    SELECT * FROM EMPLEADOS 
    WHERE sueldo > (
        SELECT AVG(salario) 
        FROM HISTORICO_SALARIOS 
        WHERE activo = 'S'
    );
END;



