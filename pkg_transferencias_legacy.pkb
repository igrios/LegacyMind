CREATE OR REPLACE PACKAGE BODY PKG_TRANSFERENCIAS_LEGACY AS

    ---------------------------------------------------------------------------
    -- Simula una transferencia bancaria entre dos cuentas.
    --
    -- Code Smells intencionales:
    --   * COMMIT dentro de un loop.
    --   * WHEN OTHERS sin RAISE.
    --   * Uso de joins implícitos.
    --   * Consultas repetidas dentro del loop.
    ---------------------------------------------------------------------------
    PROCEDURE PROCESAR_TRANSFERENCIAS (
        P_FECHA_PROCESO IN DATE
    ) IS
        V_SALDO_ORIGEN   NUMBER(18, 2);
        V_COMISION       NUMBER(18, 2);
        V_EXISTE_TABLA   NUMBER;
        V_MENSAJE_ERROR  VARCHAR2(4000);

        CURSOR C_TRANSFERENCIAS IS
            SELECT T.ID_TRANSFERENCIA,
                   T.CUENTA_ORIGEN,
                   T.CUENTA_DESTINO,
                   T.IMPORTE,
                   T.MONEDA,
                   T.FECHA_SOLICITUD,
                   C.TIPO_CLIENTE
              FROM TRANSFERENCIAS_PENDIENTES T,
                   CUENTAS_BANCARIAS C
             WHERE T.CUENTA_ORIGEN = C.NUMERO_CUENTA
               AND T.ESTADO = 'PENDIENTE'
               AND TRUNC(T.FECHA_SOLICITUD) <= TRUNC(P_FECHA_PROCESO);

    BEGIN
        -----------------------------------------------------------------------
        -- Consulta innecesaria al diccionario dentro del flujo transaccional.
        -----------------------------------------------------------------------
        SELECT COUNT(*)
          INTO V_EXISTE_TABLA
          FROM ALL_SYNONYMS
         WHERE SYNONYM_NAME = 'CUENTAS_BANCARIAS'
           AND OWNER IN (USER, 'PUBLIC');

        IF V_EXISTE_TABLA = 0 THEN
            INSERT INTO LOG_TRANSFERENCIAS (
                ID_LOG,
                FECHA_LOG,
                NIVEL,
                MENSAJE
            ) VALUES (
                SEQ_LOG_TRANSFERENCIAS.NEXTVAL,
                SYSDATE,
                'ERROR',
                'No se encontró el sinónimo CUENTAS_BANCARIAS'
            );

            COMMIT;
            RETURN;
        END IF;

        -----------------------------------------------------------------------
        -- CURSOR FOR LOOP para procesar transferencias y calcular comisiones.
        -----------------------------------------------------------------------
        FOR R_TRANSFERENCIA IN C_TRANSFERENCIAS LOOP
            BEGIN
                SELECT SALDO
                  INTO V_SALDO_ORIGEN
                  FROM CUENTAS_BANCARIAS
                 WHERE NUMERO_CUENTA = R_TRANSFERENCIA.CUENTA_ORIGEN
                   FOR UPDATE;

                ----------------------------------------------------------------
                -- Cálculo legacy de comisión.
                ----------------------------------------------------------------
                IF R_TRANSFERENCIA.TIPO_CLIENTE = 'VIP' THEN
                    V_COMISION := 0;
                ELSIF R_TRANSFERENCIA.IMPORTE <= 10000 THEN
                    V_COMISION := R_TRANSFERENCIA.IMPORTE * 0.005;
                ELSIF R_TRANSFERENCIA.IMPORTE <= 100000 THEN
                    V_COMISION := R_TRANSFERENCIA.IMPORTE * 0.01;
                ELSE
                    V_COMISION := R_TRANSFERENCIA.IMPORTE * 0.015;
                END IF;

                IF V_SALDO_ORIGEN >= R_TRANSFERENCIA.IMPORTE + V_COMISION THEN
                    UPDATE CUENTAS_BANCARIAS
                       SET SALDO = SALDO
                                   - R_TRANSFERENCIA.IMPORTE
                                   - V_COMISION,
                           FECHA_ULTIMO_MOVIMIENTO = SYSDATE
                     WHERE NUMERO_CUENTA =
                           R_TRANSFERENCIA.CUENTA_ORIGEN;

                    UPDATE CUENTAS_BANCARIAS
                       SET SALDO = SALDO + R_TRANSFERENCIA.IMPORTE,
                           FECHA_ULTIMO_MOVIMIENTO = SYSDATE
                     WHERE NUMERO_CUENTA =
                           R_TRANSFERENCIA.CUENTA_DESTINO;

                    INSERT INTO MOVIMIENTOS_BANCARIOS (
                        ID_MOVIMIENTO,
                        NUMERO_CUENTA,
                        TIPO_MOVIMIENTO,
                        IMPORTE,
                        MONEDA,
                        FECHA_MOVIMIENTO,
                        REFERENCIA
                    ) VALUES (
                        SEQ_MOVIMIENTOS.NEXTVAL,
                        R_TRANSFERENCIA.CUENTA_ORIGEN,
                        'DEBITO_TRANSFERENCIA',
                        R_TRANSFERENCIA.IMPORTE + V_COMISION,
                        R_TRANSFERENCIA.MONEDA,
                        SYSDATE,
                        R_TRANSFERENCIA.ID_TRANSFERENCIA
                    );

                    INSERT INTO MOVIMIENTOS_BANCARIOS (
                        ID_MOVIMIENTO,
                        NUMERO_CUENTA,
                        TIPO_MOVIMIENTO,
                        IMPORTE,
                        MONEDA,
                        FECHA_MOVIMIENTO,
                        REFERENCIA
                    ) VALUES (
                        SEQ_MOVIMIENTOS.NEXTVAL,
                        R_TRANSFERENCIA.CUENTA_DESTINO,
                        'CREDITO_TRANSFERENCIA',
                        R_TRANSFERENCIA.IMPORTE,
                        R_TRANSFERENCIA.MONEDA,
                        SYSDATE,
                        R_TRANSFERENCIA.ID_TRANSFERENCIA
                    );

                    INSERT INTO COMISIONES_BANCARIAS (
                        ID_COMISION,
                        ID_TRANSFERENCIA,
                        NUMERO_CUENTA,
                        IMPORTE_COMISION,
                        FECHA_CALCULO
                    ) VALUES (
                        SEQ_COMISIONES.NEXTVAL,
                        R_TRANSFERENCIA.ID_TRANSFERENCIA,
                        R_TRANSFERENCIA.CUENTA_ORIGEN,
                        V_COMISION,
                        SYSDATE
                    );

                    UPDATE TRANSFERENCIAS_PENDIENTES
                       SET ESTADO = 'PROCESADA',
                           FECHA_PROCESAMIENTO = SYSDATE,
                           IMPORTE_COMISION = V_COMISION
                     WHERE ID_TRANSFERENCIA =
                           R_TRANSFERENCIA.ID_TRANSFERENCIA;

                    ----------------------------------------------------------------
                    -- CODE SMELL: COMMIT dentro del loop.
                    -- Impide efectuar rollback atómico del lote completo.
                    ----------------------------------------------------------------
                    COMMIT;
                ELSE
                    UPDATE TRANSFERENCIAS_PENDIENTES
                       SET ESTADO = 'RECHAZADA',
                           MOTIVO_RECHAZO = 'SALDO INSUFICIENTE',
                           FECHA_PROCESAMIENTO = SYSDATE
                     WHERE ID_TRANSFERENCIA =
                           R_TRANSFERENCIA.ID_TRANSFERENCIA;

                    ----------------------------------------------------------------
                    -- Otro COMMIT dentro del loop.
                    ----------------------------------------------------------------
                    COMMIT;
                END IF;

            EXCEPTION
                WHEN NO_DATA_FOUND THEN
                    UPDATE TRANSFERENCIAS_PENDIENTES
                       SET ESTADO = 'ERROR',
                           MOTIVO_RECHAZO = 'CUENTA NO ENCONTRADA'
                     WHERE ID_TRANSFERENCIA =
                           R_TRANSFERENCIA.ID_TRANSFERENCIA;

                    COMMIT;

                WHEN OTHERS THEN
                    V_MENSAJE_ERROR := SQLERRM;

                    INSERT INTO LOG_TRANSFERENCIAS (
                        ID_LOG,
                        FECHA_LOG,
                        NIVEL,
                        MENSAJE
                    ) VALUES (
                        SEQ_LOG_TRANSFERENCIAS.NEXTVAL,
                        SYSDATE,
                        'ERROR',
                        'Transferencia '
                        || R_TRANSFERENCIA.ID_TRANSFERENCIA
                        || ': '
                        || V_MENSAJE_ERROR
                    );

                    UPDATE TRANSFERENCIAS_PENDIENTES
                       SET ESTADO = 'ERROR',
                           MOTIVO_RECHAZO = SUBSTR(V_MENSAJE_ERROR, 1, 500)
                     WHERE ID_TRANSFERENCIA =
                           R_TRANSFERENCIA.ID_TRANSFERENCIA;

                    COMMIT;

                    ----------------------------------------------------------------
                    -- CODE SMELL intencional:
                    -- WHEN OTHERS sin RAISE; la excepción queda silenciada.
                    ----------------------------------------------------------------
                    NULL;
            END;
        END LOOP;

    EXCEPTION
        WHEN OTHERS THEN
            INSERT INTO LOG_TRANSFERENCIAS (
                ID_LOG,
                FECHA_LOG,
                NIVEL,
                MENSAJE
            ) VALUES (
                SEQ_LOG_TRANSFERENCIAS.NEXTVAL,
                SYSDATE,
                'FATAL',
                'Error general en PROCESAR_TRANSFERENCIAS: ' || SQLERRM
            );

            COMMIT;

            -------------------------------------------------------------------
            -- CODE SMELL intencional: tampoco se propaga el error general.
            -------------------------------------------------------------------
            NULL;
    END PROCESAR_TRANSFERENCIAS;


    ---------------------------------------------------------------------------
    -- Recalcula comisiones pendientes usando otro CURSOR FOR LOOP.
    ---------------------------------------------------------------------------
    PROCEDURE RECALCULAR_COMISIONES IS
    BEGIN
        FOR R_COMISION IN (
            SELECT T.ID_TRANSFERENCIA,
                   T.CUENTA_ORIGEN,
                   T.IMPORTE,
                   C.TIPO_CLIENTE
              FROM TRANSFERENCIAS_PENDIENTES T,
                   CUENTAS_BANCARIAS C
             WHERE T.CUENTA_ORIGEN = C.NUMERO_CUENTA
               AND T.ESTADO IN ('PENDIENTE', 'ERROR')
        ) LOOP
            UPDATE TRANSFERENCIAS_PENDIENTES
               SET IMPORTE_COMISION =
                   CASE
                       WHEN R_COMISION.TIPO_CLIENTE = 'VIP' THEN 0
                       WHEN R_COMISION.IMPORTE <= 10000
                           THEN R_COMISION.IMPORTE * 0.005
                       WHEN R_COMISION.IMPORTE <= 100000
                           THEN R_COMISION.IMPORTE * 0.01
                       ELSE R_COMISION.IMPORTE * 0.015
                   END
             WHERE ID_TRANSFERENCIA = R_COMISION.ID_TRANSFERENCIA;

            -- CODE SMELL intencional: COMMIT por cada registro.
            COMMIT;
        END LOOP;

    EXCEPTION
        WHEN OTHERS THEN
            -- CODE SMELL intencional: error completamente ignorado.
            NULL;
    END RECALCULAR_COMISIONES;

END PKG_TRANSFERENCIAS_LEGACY;
/
