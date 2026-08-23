package com.ignacio.legacyanalyzer.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.domain.model.CursorMetadata;
import com.ignacio.legacyanalyzer.domain.services.semantic.CursorSemanticExtractor;

class CursorSemanticExtractorTest {

    private CursorSemanticExtractor extractor;

    @BeforeEach
    void setup() {

        extractor =
                new CursorSemanticExtractor();
    }

    @Test
    void should_detect_cursor_metadata() {

        String sourceCode = """
            CREATE OR REPLACE PROCEDURE SP_PROCESAR_VENTAS IS

                CURSOR c_ventas IS
                    SELECT *
                    FROM VENTAS
                    FOR UPDATE;

            BEGIN

                NULL;

            END;
            """;

        List<CursorMetadata> cursors =
                extractor.extractCursors(sourceCode);

        assertEquals(1, cursors.size());

        CursorMetadata cursor =
                cursors.get(0);

        assertEquals(
                "C_VENTAS",
                cursor.cursorName());

        assertTrue(
                cursor.referencedTables()
                        .contains("VENTAS"));

        assertTrue(
                cursor.forUpdate());

        assertFalse(
                cursor.bulkCollect());
    }


    @Test
void should_detect_bulk_collect() {

    String sourceCode = """
        CREATE OR REPLACE PROCEDURE SP_BATCH IS

            CURSOR c_ventas IS
                SELECT *
                FROM VENTAS;

            TYPE t_ventas IS TABLE OF VENTAS%ROWTYPE;

            v_ventas t_ventas;

        BEGIN

            OPEN c_ventas;

            FETCH c_ventas
            BULK COLLECT INTO v_ventas;

            CLOSE c_ventas;

        END;
        """;

    List<CursorMetadata> cursors =
            extractor.extractCursors(sourceCode);

    assertEquals(1, cursors.size());

    CursorMetadata cursor =
            cursors.get(0);

    assertEquals(
            "C_VENTAS",
            cursor.cursorName());

    assertTrue(
            cursor.bulkCollect());
}


@Test
void should_detect_forall_batch_processing() {

    String sourceCode = """
        CREATE OR REPLACE PROCEDURE SP_BATCH_PROCESS IS

            CURSOR c_ventas IS
                SELECT *
                FROM VENTAS
                FOR UPDATE;

            TYPE t_ventas IS TABLE OF VENTAS%ROWTYPE;

            v_ventas t_ventas;

        BEGIN

            OPEN c_ventas;

            FETCH c_ventas
            BULK COLLECT INTO v_ventas;

            FORALL i IN 1 .. v_ventas.COUNT

                UPDATE VENTAS
                SET estado = 'PROCESADO'
                WHERE id = v_ventas(i).id;

            CLOSE c_ventas;

        END;
        """;

    List<CursorMetadata> cursors =
            extractor.extractCursors(sourceCode);

    assertEquals(
            1,
            cursors.size());

    CursorMetadata cursor =
            cursors.get(0);

    // =========================================
    // CURSOR NAME
    // =========================================

    assertEquals(
            "C_VENTAS",
            cursor.cursorName());

    // =========================================
    // REFERENCED TABLES
    // =========================================

    assertTrue(
            cursor.referencedTables()
                    .contains("VENTAS"));

    // =========================================
    // BULK COLLECT
    // =========================================

    assertTrue(
            cursor.bulkCollect());

    // =========================================
    // FOR UPDATE
    // =========================================

    assertTrue(
            cursor.forUpdate());

    // =========================================
    // FORALL
    // =========================================

    assertTrue(
            cursor.forall());
}


}
