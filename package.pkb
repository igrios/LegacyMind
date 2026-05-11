CREATE OR REPLACE PACKAGE BODY pkg_empleados AS

    -- Implementación del procedimiento para aumentar salario
    PROCEDURE aumentar_salario(p_id_emp NUMBER, p_porcentaje NUMBER) IS
    BEGIN
        UPDATE empleados 
        SET salario = salario * (1 + p_porcentaje / 100)
        WHERE id_empleado = p_id_emp;
        
        COMMIT;
    END aumentar_salario;

    -- Implementación de la función para consultar salario
    FUNCTION obtener_salario(p_id_emp NUMBER) RETURN NUMBER IS
        v_salario NUMBER;
    BEGIN
        SELECT salario INTO v_salario 
        FROM empleados 
        WHERE id_empleado = p_id_emp;
        
        RETURN v_salario;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RETURN NULL;
        WHEN OTHERS THEN
            -- Considera registrar el error aquí
            RAISE;
    END obtener_salario;

END pkg_empleados;
/
